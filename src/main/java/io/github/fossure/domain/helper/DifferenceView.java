package io.github.fossure.domain.helper;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import io.github.fossure.domain.Library;
import org.apache.commons.lang3.StringUtils;

public class DifferenceView implements Serializable {

    public static class VersionChange {

        Set<Library> librarySet = new HashSet<>(3);
    }

    private List<Library> sameLibraries;
    private List<Library> addedLibraries;
    private List<Library> removedLibraries;
    private List<Library> firstProjectNewLibraries;
    private List<Library> secondProjectNewLibraries;

    public DifferenceView(
        List<Library> sameLibraries,
        List<Library> addedLibraries,
        List<Library> removedLibraries,
        List<Library> firstProjectNewLibraries,
        List<Library> secondProjectNewLibraries
    ) {
        this.sameLibraries = sameLibraries;
        this.addedLibraries = addedLibraries;
        this.removedLibraries = removedLibraries;
        this.firstProjectNewLibraries = firstProjectNewLibraries;
        this.secondProjectNewLibraries = secondProjectNewLibraries;
    }

    /**
     * @param a First list
     * @param b Second list
     * @return A new Set with libraries
     */
    public List<Library> versionChange(List<Library> a, List<Library> b) {
        List<Library> removedDuplicatesFromB = b.stream().filter(a::contains).collect(Collectors.toList());

        Map<String, VersionChange> newLibraryVersions = new HashMap<>();

        for (Library libraryA : a) {
            for (Library libraryRemovedDuplicatesLibrary : removedDuplicatesFromB) {
                if (isEqualLibraryNameAndType(libraryA, libraryRemovedDuplicatesLibrary)) {
                    DifferenceView.VersionChange versionChange = new DifferenceView.VersionChange();
                }
            }
        }

        return new ArrayList<>(1);
    }

    /**
     * Check if Namespace, Name and Type of two libraries are the same.
     *
     * @param a First library entity
     * @param b Second library entity
     * @return true, if it has the same Namespace, Name and Type, otherwise false.
     */
    public static boolean isEqualLibraryNameAndType(Library a, Library b) {
        if (!StringUtils.isBlank(a.getNamespace()) && !StringUtils.isBlank(b.getNamespace())) {
            return a.getNamespace().equals(b.getNamespace()) && a.getName().equals(b.getName()) && a.getType().equals(b.getType());
        } else if (StringUtils.isBlank(a.getNamespace()) && StringUtils.isBlank(b.getNamespace())) {
            return a.getName().equals(b.getName()) && a.getType().equals(b.getType());
        } else {
            return false;
        }
    }

    /**
     * Create a label for a library from Namespace, Name and Type.
     * If the Namespace is not present, then only the Name and Type will be taken.
     *
     * @return Label for the library.
     */
    public static String createLabel(Library library) {
        return StringUtils.isBlank(library.getNamespace())
            ? library.getName() + ":" + library.getType().getValue()
            : library.getNamespace() + ":" + library.getName() + ":" + library.getType().getValue();
    }

    public List<Library> getSameLibraries() {
        return sameLibraries;
    }

    public void setSameLibraries(List<Library> sameLibraries) {
        this.sameLibraries = sameLibraries;
    }

    public List<Library> getAddedLibraries() {
        return addedLibraries;
    }

    public void setAddedLibraries(List<Library> addedLibraries) {
        this.addedLibraries = addedLibraries;
    }

    public List<Library> getRemovedLibraries() {
        return removedLibraries;
    }

    public void setRemovedLibraries(List<Library> removedLibraries) {
        this.removedLibraries = removedLibraries;
    }

    public List<Library> getFirstProjectNewLibraries() {
        return firstProjectNewLibraries;
    }

    public void setFirstProjectNewLibraries(List<Library> firstProjectNewLibraries) {
        this.firstProjectNewLibraries = firstProjectNewLibraries;
    }

    public List<Library> getSecondProjectNewLibraries() {
        return secondProjectNewLibraries;
    }

    public void setSecondProjectNewLibraries(List<Library> secondProjectNewLibraries) {
        this.secondProjectNewLibraries = secondProjectNewLibraries;
    }

    @Override
    public String toString() {
        return "DifferenceView{" + "addedLibraries='" + addedLibraries + "'" + ", removedLibraries=" + removedLibraries + "'" + '}';
    }
}
