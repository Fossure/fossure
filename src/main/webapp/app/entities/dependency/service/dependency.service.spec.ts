import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import dayjs from 'dayjs/esm';

import { DATE_FORMAT } from 'app/config/input.constants';
import { IDependency, Dependency } from '../dependency.model';

import { DependencyService } from './dependency.service';

describe('Dependency Service', () => {
  let service: DependencyService;
  let httpMock: HttpTestingController;
  let elemDefault: IDependency;
  let expectedResult: IDependency | IDependency[] | boolean | null;
  let currentDate: dayjs.Dayjs;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(DependencyService);
    httpMock = TestBed.inject(HttpTestingController);
    currentDate = dayjs();

    elemDefault = {
      id: 0,
      addedDate: currentDate,
      addedManually: false,
      hideForPublishing: false,
    };
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = Object.assign(
        {
          addedDate: currentDate.format(DATE_FORMAT),
        },
        elemDefault
      );

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(elemDefault);
    });

    it('should create a Dependency', () => {
      const returnedFromService = Object.assign(
        {
          id: 0,
          addedDate: currentDate.format(DATE_FORMAT),
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          addedDate: currentDate,
        },
        returnedFromService
      );

      service.create(new Dependency()).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Dependency', () => {
      const returnedFromService = Object.assign(
        {
          id: 1,
          addedDate: currentDate.format(DATE_FORMAT),
          addedManually: true,
          hideForPublishing: true,
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          addedDate: currentDate,
        },
        returnedFromService
      );

      service.update(expected).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Dependency', () => {
      const patchObject = Object.assign(
        {
          addedManually: true,
        },
        new Dependency()
      );

      const returnedFromService = Object.assign(patchObject, elemDefault);

      const expected = Object.assign(
        {
          addedDate: currentDate,
        },
        returnedFromService
      );

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Dependency', () => {
      const returnedFromService = Object.assign(
        {
          id: 1,
          addedDate: currentDate.format(DATE_FORMAT),
          addedManually: true,
          hideForPublishing: true,
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          addedDate: currentDate,
        },
        returnedFromService
      );

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toContainEqual(expected);
    });

    it('should delete a Dependency', () => {
      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult);
    });

    describe('addDependencyToCollectionIfMissing', () => {
      it('should add a Dependency to an empty array', () => {
        const dependency: IDependency = { id: 123 };
        expectedResult = service.addDependencyToCollectionIfMissing([], dependency);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(dependency);
      });

      it('should not add a Dependency to an array that contains it', () => {
        const dependency: IDependency = { id: 123 };
        const dependencyCollection: IDependency[] = [
          {
            ...dependency,
          },
          { id: 456 },
        ];
        expectedResult = service.addDependencyToCollectionIfMissing(dependencyCollection, dependency);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Dependency to an array that doesn't contain it", () => {
        const dependency: IDependency = { id: 123 };
        const dependencyCollection: IDependency[] = [{ id: 456 }];
        expectedResult = service.addDependencyToCollectionIfMissing(dependencyCollection, dependency);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(dependency);
      });

      it('should add only unique Dependency to an array', () => {
        const dependencyArray: IDependency[] = [{ id: 123 }, { id: 456 }, { id: 25346 }];
        const dependencyCollection: IDependency[] = [{ id: 123 }];
        expectedResult = service.addDependencyToCollectionIfMissing(dependencyCollection, ...dependencyArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const dependency: IDependency = { id: 123 };
        const dependency2: IDependency = { id: 456 };
        expectedResult = service.addDependencyToCollectionIfMissing([], dependency, dependency2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(dependency);
        expect(expectedResult).toContain(dependency2);
      });

      it('should accept null and undefined values', () => {
        const dependency: IDependency = { id: 123 };
        expectedResult = service.addDependencyToCollectionIfMissing([], null, dependency, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(dependency);
      });

      it('should return initial array if no Dependency is added', () => {
        const dependencyCollection: IDependency[] = [{ id: 123 }];
        expectedResult = service.addDependencyToCollectionIfMissing(dependencyCollection, undefined, null);
        expect(expectedResult).toEqual(dependencyCollection);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
