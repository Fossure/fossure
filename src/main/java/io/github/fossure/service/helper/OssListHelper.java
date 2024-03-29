package io.github.fossure.service.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.fossure.domain.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import io.github.fossure.config.Constants;
import io.github.fossure.domain.Project;

public class OssListHelper {

    static class RiskColor {

        static final String PERMISSIVE = "#DAF7A688";
        static final String LIMITED_COPYLEFT = "#FFC30066";
        static final String STRONG_COPYLEFT = "#FF573366";
        static final String COMMERCIAL = "#20a8d866";
        static final String FORBIDDEN = "#c7000066";
        static final String PROPRIETARY_FREE = "#9933ff66";
        static final String UNKNOWN = "#eeeeee88";

        static String getColorByRiskName(String risk) {
            switch (risk) {
                case "Permissive":
                    return PERMISSIVE;
                case "Limited Copyleft":
                    return LIMITED_COPYLEFT;
                case "Strong Copyleft":
                    return STRONG_COPYLEFT;
                case "Commercial":
                    return COMMERCIAL;
                case "Forbidden":
                    return FORBIDDEN;
                case "Proprietary Free":
                    return PROPRIETARY_FREE;
                default:
                    return UNKNOWN;
            }
        }
    }

    private final Logger log = LoggerFactory.getLogger(OssListHelper.class);

    private static final String DEFAULT_HTML_TEMPLATE = "classpath:templates/ossList/default.html";
    private static final String NAME_DELIMITER = "_";

    private final Map<String, List<Library>> libraries;

    private File htmlTemplate;
    private Project project;
    private String html;
    private String tableHeader = "";
    private boolean withNamespace = false;

    public OssListHelper(List<Library> libraries) {
        long numberOfLibrariesWithNamespace = libraries.stream().filter(library -> !library.getNamespace().isEmpty()).count();
        if (numberOfLibrariesWithNamespace > 0) this.withNamespace = true;
        this.libraries = groupLibraryDuplicates(libraries);
    }

    public OssListHelper(Project project, List<Library> libraries, boolean distinct) throws FileNotFoundException {
        this.project = project;

        long numberOfLibrariesWithNamespace = libraries.stream().filter(library -> !library.getNamespace().isEmpty()).count();
        if (numberOfLibrariesWithNamespace > 0) this.withNamespace = true;

        if (distinct) {
            this.libraries = groupLibraryDuplicates(libraries);
        } else {
            Map<String, List<Library>> libraryMap = new LinkedHashMap<>(128);

            // TODO add library.type to libraryName to have a unique key name
            libraries.forEach(library -> {
                String libraryName = !library.getNamespace().isEmpty()
                    ? library.getNamespace() + NAME_DELIMITER + library.getName() + NAME_DELIMITER + library.getVersion()
                    : library.getName() + NAME_DELIMITER + library.getVersion();

                libraryName = libraryName.replaceAll(" ", NAME_DELIMITER);
                List<Library> newLibraryList = new ArrayList<>();
                newLibraryList.add(library);
                libraryMap.put(libraryName, newLibraryList);
            });
            this.libraries = libraryMap;
        }
        this.htmlTemplate = ResourceUtils.getFile(DEFAULT_HTML_TEMPLATE);
    }

    public OssListHelper(Project project, List<Library> libraries, String htmlTemplate) throws FileNotFoundException {
        this.project = project;
        this.libraries = groupLibraryDuplicates(libraries);
        this.htmlTemplate = ResourceUtils.getFile(htmlTemplate);
    }

    public Map<String, List<Library>> groupLibraryDuplicates(List<Library> libraries) {
        Map<String, List<Library>> libraryMap = new LinkedHashMap<>(128);

        // TODO add library.type to libraryName to have a unique key name
        libraries.forEach(library -> {
            String libraryName = !library.getNamespace().isEmpty()
                ? library.getNamespace() + NAME_DELIMITER + library.getName()
                : library.getName();

            libraryName = libraryName.replaceAll(" ", NAME_DELIMITER);

            if (libraryMap.containsKey(libraryName)) {
                libraryMap.get(libraryName).add(library);
            } else {
                List<Library> newLibraryList = new ArrayList<>();
                newLibraryList.add(library);
                libraryMap.put(libraryName, newLibraryList);
            }
        });

        return libraryMap;
    }

    public byte[] createPublishCsv() throws IOException {
        Deque<String> headers = new ArrayDeque<>(3);
        if (withNamespace) {
            headers.add("Namespace");
        }
        headers.add("Name");
        headers.add("License");


        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader(headers.toArray(new String[0]))
            .build();

        StringBuilder csvBuilder = new StringBuilder(32768);
        try (CSVPrinter csvPrinter = new CSVPrinter(csvBuilder, csvFormat)) {
            for (Map.Entry<String, List<Library>> entry : libraries.entrySet()) {
                List<Library> value = entry.getValue();
                int counter = 1;
                List<String> licenses = new ArrayList<>(4);
                for (Library library : value) {
                    String currentLicenses = library
                        .getLicenseToPublishes()
                        .stream()
                        .map(License::getShortIdentifier)
                        .collect(Collectors.joining(" AND "));
                    if (!licenses.contains(currentLicenses)) {
                        licenses.add(" (" + counter + ")");
                        licenses.add(currentLicenses);
                    }
                    counter++;
                }

                // remove first element (enumeration) if library has only one license
                if (licenses.size() <= 2) licenses.remove(0);

                if (withNamespace) {
                    csvPrinter.printRecord(value.get(0).getNamespace(), value.get(0).getName(), String.join("", licenses).trim());
                } else {
                    csvPrinter.printRecord(value.get(0).getName(), String.join("", licenses));
                }

                csvPrinter.flush();
            }
        }

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public void createHml(boolean distinct) {
        try {
            html = new String(Files.readAllBytes(htmlTemplate.toPath()));
        } catch (IOException e) {
            log.error("Error while reading HTML template : {}", e.getMessage());
        }

        html = html.replace("%{project}%", project.getName() + " " + project.getVersion());
        html = addTableContent(createTableContent(distinct));
        html = html.replace("%{tableHeader}%", tableHeader);
    }

    private String addTableContent(String content) {
        return html.replace("%{tableContent}%", content);
    }

    public String createTableContent(boolean distinct) {
        StringBuilder tableContent = new StringBuilder();

        libraries.forEach((key, value) -> {
            int counter = 1;

            String namespace = value.get(0).getNamespace();
            String name = value.get(0).getName();
            List<String> licenses = new ArrayList<>(4);
            LicenseRisk licenseRisk = value.get(0).getLicenseRisk(value.get(0).getLicenseToPublishes());
            String risk = licenseRisk != null ? licenseRisk.getName() : "Unknown";

            for (Library library : value) {
                if (distinct) {
                    String currentlicenses = library
                        .getLicenseToPublishes()
                        .stream()
                        .map(License::getShortIdentifier)
                        .collect(Collectors.joining(" AND "));
                    if (!licenses.contains(currentlicenses)) {
                        licenses.add(" <span class=\"enumeration\">" + counter + "</span> ");
                        licenses.add(currentlicenses);
                    }
                    counter++;
                }
            }

            if (distinct) {
                // remove first element (enumeration) if library has only one license
                if (licenses.size() <= 2) licenses.remove(0);
                addTableHeader(withNamespace);
                tableContent.append(addTableRow(namespace, name, String.join("", licenses), withNamespace));
            } else {
                addTableHeaderDefault(withNamespace);
                tableContent.append(
                    addTableRowDefault(
                        namespace,
                        name,
                        value.get(0).getVersion() != null ? value.get(0).getVersion() : "",
                        value.get(0).getType() != null ? value.get(0).getType().getValue() : "",
                        //value.get(0).getLicenseToPublishes().stream().map(License::getShortIdentifier).collect(Collectors.joining(" AND ")),
                        value.get(0).getLicenses() != null ? value.get(0).printLinkedLicenses() : "",
                        risk,
                        value.get(0).getLicenseUrl() != null &&
                            !StringUtils.isBlank(value.get(0).getLicenseUrl()) &&
                            !value.get(0).getLicenseUrl().equalsIgnoreCase(Constants.NO_URL)
                            ? value.get(0).getLicenseUrl()
                            : value.get(0).getLicenseToPublishes().stream().map(License::getUrl).collect(Collectors.joining(", ")),
                        value.get(0).getSourceCodeUrl() != null ? value.get(0).getSourceCodeUrl() : "",
                        withNamespace
                    )
                );
            }
        });

        return tableContent.toString();
    }

    private void addTableHeader(boolean withNamespace) {
        if (withNamespace) {
            this.tableHeader = "<tr>\n" + "<th>Namespace</th>\n" + "<th>Name</th>\n" + "<th>License</th>\n" + "</tr>";
        } else {
            this.tableHeader = "<tr>\n" + "<th>Name</th>\n" + "<th>License</th>\n" + "</tr>";
        }
    }

    private void addTableHeaderDefault(boolean withNamespace) {
        if (withNamespace) {
            this.tableHeader =
                "<tr>\n" +
                "<th>Namespace</th>\n" +
                "<th>Name</th>\n" +
                "<th>Version</th>\n" +
                "<th>Type</th>\n" +
                "<th>License</th>\n" +
                "<th>LicenseRisk</th>\n" +
                "<th>LicenseUrl</th>\n" +
                "<th>SourceCodeUrl</th>\n" +
                "</tr>";
        } else {
            this.tableHeader =
                "<tr>\n" +
                "<th>Name</th>\n" +
                "<th>Version</th>\n" +
                "<th>Type</th>\n" +
                "<th>License</th>\n" +
                "<th>LicenseRisk</th>\n" +
                "<th>LicenseUrl</th>\n" +
                "<th>SourceCodeUrl</th>\n" +
                "</tr>";
        }
    }

    private String addTableRow(String namespace, String name, String licenses, boolean withNamespace) {
        String template;

        if (withNamespace) {
            template = "<tr>\n" + "<td>%{namespace}%</td>\n" + "<td>%{name}%</td>\n" + "<td>%{licenses}%</td>\n" + "</tr>";
        } else {
            template = "<tr>\n" + "<td>%{name}%</td>\n" + "<td>%{licenses}%</td>\n" + "</tr>";
        }

        return template.replace("%{namespace}%", namespace).replace("%{name}%", name).replace("%{licenses}%", licenses);
    }

    private String addTableRowDefault(
        String namespace,
        String name,
        String version,
        String type,
        String licenses,
        String licenseRisk,
        String licenseUrl,
        String sourceCodeUrl,
        boolean withNamespace
    ) {
        String template;

        if (withNamespace) {
            template =
                "<tr>\n" +
                "<td>%{namespace}%</td>\n" +
                "<td>%{name}%</td>\n" +
                "<td>%{version}%</td>\n" +
                "<td>%{type}%</td>\n" +
                "<td style=\"background-color:" +
                RiskColor.getColorByRiskName(licenseRisk) +
                "\">%{licenses}%</td>\n" +
                "<td style=\"background-color:" +
                RiskColor.getColorByRiskName(licenseRisk) +
                "\">%{licenseRisk}%</td>\n" +
                "<td>%{licenseUrl}%</td>\n" +
                "<td>%{sourceCodeUrl}%</td>\n" +
                "</tr>";
        } else {
            template =
                "<tr>\n" +
                "<td>%{name}%</td>\n" +
                "<td>%{version}%</td>\n" +
                "<td>%{type}%</td>\n" +
                "<td style=\"background-color:" +
                RiskColor.getColorByRiskName(licenseRisk) +
                "\">%{licenses}%</td>\n" +
                "<td style=\"background-color:" +
                RiskColor.getColorByRiskName(licenseRisk) +
                "\">%{licenseRisk}%</td>\n" +
                "<td>%{licenseUrl}%</td>\n" +
                "<td>%{sourceCodeUrl}%</td>\n" +
                "</tr>";
        }

        return template
            .replace("%{namespace}%", namespace)
            .replace("%{name}%", name)
            .replace("%{version}%", version)
            .replace("%{type}%", type)
            .replace("%{licenses}%", licenses)
            .replace("%{licenseRisk}%", licenseRisk)
            .replace("%{licenseUrl}%", licenseUrl)
            .replace("%{sourceCodeUrl}%", sourceCodeUrl);
    }

    public void createFullHtml(List<String> requirementsLookup) {
        List<List<String>> data = new ArrayList<>();

        List<String> header = new ArrayList<>(
            Arrays.asList("Namespace", "Name", "Version", "License", "LicenseRisk", "LicensesTotal", "Comment", "ComplianceComment")
        );

        header.addAll(requirementsLookup);
        Set<String> totalLicenses = new HashSet<>();

        libraries.forEach((key, value) -> {

            for (Library dependency : value) {
                String namespace = dependency.getNamespace();
                String name = dependency.getName();
                String version = dependency.getVersion();

                // Changing the button value when is pressed is defined in the collapse JS function in the ossList/default.html
                String beforeData =
                    "<button type=\"button\" onclick=\"collapse(this)\" class=\"collapsible\">Show comment</button>\n" +
                    "<div class=\"content\">\n" +
                    "<p>";
                String afterData = "</p>\n" + "</div>";

                String comment = !StringUtils.isBlank(dependency.getComment())
                    ? beforeData + dependency.getComment() + afterData
                    : "No comment";

                String complianceComment = !StringUtils.isBlank(dependency.getComplianceComment())
                    ? beforeData + dependency.getComplianceComment() + afterData
                    : "No comment";

                String licenseRisk = dependency.getLicenseRisk(dependency.getLicenseToPublishes()).getName();
                List<String> requirements = new ArrayList<>(16);

                for (String ignored : requirementsLookup) {
                    requirements.add("");
                }

                dependency.getLicenses().forEach(e -> totalLicenses.add(e.getLicense().getShortIdentifier()));

                if (dependency.getLicenseToPublishes() != null && !dependency.getLicenseToPublishes().isEmpty()) {
                    for (License licenseToPublish : dependency.getLicenseToPublishes()) {
                        if (licenseToPublish.getRequirements() != null) {
                            for (Requirement requirement : licenseToPublish.getRequirements()) {
                                if (requirementsLookup.contains(requirement.getShortText())) {
                                    requirements.remove(requirementsLookup.indexOf(requirement.getShortText()));
                                    requirements.add(requirementsLookup.indexOf(requirement.getShortText()), "X");
                                }
                            }
                        }
                    }
                }

                List<String> resultList = new ArrayList<>();
                Collections.addAll(
                    resultList,
                    namespace,
                    name,
                    "V" + version,
                    dependency.printLinkedLicenses(),
                    licenseRisk,
                    String.valueOf(totalLicenses.size()),
                    comment,
                    complianceComment
                );

                resultList.addAll(requirements);

                data.add(resultList);
            }
        });

        data.forEach(e -> {});

        this.tableHeader += "<tr>\n";
        header.forEach(e -> this.tableHeader += "<th>" + e + "</th>\n");
        this.tableHeader += "</tr>";

        StringBuilder content = new StringBuilder();
        data.forEach(e -> {
            content.append("<tr>\n");

            for (String ele : e) {
                content.append("<td>").append(ele).append("</td>\n");
            }

            content.append("</tr>\n");
        });

        try {
            html = new String(Files.readAllBytes(htmlTemplate.toPath()));
        } catch (IOException e) {
            log.error("Error while reading HTML template : {}", e.getMessage());
        }

        html = html.replace("%{project}%", project.getName() + " " + project.getVersion());
        html = addTableContent(content.toString());
        html = html.replace("%{tableHeader}%", tableHeader);
    }

    public String getHtml() {
        return this.html;
    }
}
