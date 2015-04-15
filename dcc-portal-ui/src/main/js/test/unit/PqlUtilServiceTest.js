// Test suite for PqlUtilService

describe('Testing PqlUtilService', function() {
  var baseUrl = "search";
  var PqlUtilService;
  var location;
  var paramName;

  beforeEach(module('icgc'));

  beforeEach(inject(function ($location, _PqlUtilService_) {
    window._gaq = [];
    PqlUtilService = _PqlUtilService_;
    paramName  = PqlUtilService.paramName;
    location = $location;
    location.url (baseUrl);
    location.search (paramName, '');
  }));

  it('Testing getRawPql() with empty pql', function() {
     var expectedPql = '';
     var testPql = PqlUtilService.getRawPql();
    
     expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: eq(test,123)', function() {
    var expectedPql = "eq(test,123)";
    location.search (paramName, expectedPql);

    var testPql = PqlUtilService.getRawPql();
    
    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: eq(donor.gender,"male")', function() {
    var category = "donor";
    var facet = "gender";
    var term = "male";
    var expectedPql = 'eq(' + category + '.' + facet + ',' + '"' + term + '"' + ')';

    PqlUtilService.addTerm (category, facet, term);

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: in(donor.gender,"male", "female")', function() {
    var category = "donor";
    var facet = "gender";
    var term1 = "male";
    var term2 = "female";
    var expectedPql = 'in(' + category + '.' + facet + ',' + '"' + term1 + '"'
      + ',' + '"' + term2 + '"' + ')';

    PqlUtilService.addTerm (category, facet, term1);
    PqlUtilService.addTerm (category, facet, term2);

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22))', function() {
    var category = "donor";
    var facet = "gender";
    var term1 = "male";
    var term2 = "female";
    var expectedPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22))';

    PqlUtilService.addTerm (category, facet, term1);
    PqlUtilService.addTerm (category, facet, term2);
    PqlUtilService.addTerm (category, facet, term2);
    PqlUtilService.addTerm (category, "age", 22);

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var category = "donor";
    var facet = "gender";
    var term1 = "male";
    var term2 = "female";
    var expectedPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';

    PqlUtilService.addTerm (category, facet, term1);
    PqlUtilService.addTerm (category, facet, term2);
    PqlUtilService.addTerm (category, facet, term2);
    PqlUtilService.addTerm (category, "age", 22);
    PqlUtilService.addTerm ("mutation", "foo", "bar");

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getQuery() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var category = "donor";
    var facet = "gender";
    var term1 = "male";
    var term2 = "female";

    var expectedQuery = {
      donor: {
        gender: {
          "in": ["male", "female"]
        },
        age: {
          "in": [22]
        }
      },
      mutation: {
        foo: {
          "in": ["bar"]
        }
      }
    };

    PqlUtilService.addTerm (category, facet, term1);
    PqlUtilService.addTerm (category, facet, term2);
    PqlUtilService.addTerm (category, facet, term2);
    PqlUtilService.addTerm (category, "age", 22);
    PqlUtilService.addTerm ("mutation", "foo", "bar");

    var testQuery = PqlUtilService.getQuery();

    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing removeTerm() with "female" to be removed in pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var originalPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    location.search (paramName, originalPql);

    var expectedQuery = {
      donor: {
        gender: {
          "in": ["male"]
        },
        age: {
          "in": [22]
        }
      },
      mutation: {
        foo: {
          "in": ["bar"]
        }
      }
    };

    PqlUtilService.removeTerm ("donor", "gender", "female");

    var testQuery = PqlUtilService.getQuery();

    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing removeTerm() with both "male" and "female" to be removed in pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var originalPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    location.search (paramName, originalPql);

    var expectedQuery = {
      donor: {
        age: {
          "in": [22]
        }
      },
      mutation: {
        foo: {
          "in": ["bar"]
        }
      }
    };

    PqlUtilService.removeTerm ("donor", "gender", "female");
    PqlUtilService.removeTerm ("donor", "gender", "male");

    var testQuery = PqlUtilService.getQuery();

    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing removeFacet() with "donor.age" to be removed in pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var originalPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    location.search (paramName, originalPql);

    var expectedQuery = {
      donor: {
        gender: {
          "in": ["male", "female"]
        }
      },
      mutation: {
        foo: {
          "in": ["bar"]
        }
      }
    };

    PqlUtilService.removeFacet ("donor", "age");

    var testQuery = PqlUtilService.getQuery();

    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing Builder.build()', function() {
    var expected = 'in(donor.gender,"male","female")';
    var builder = PqlUtilService.getBuilder ();

    builder.addTerm ("donor", "gender", "male");
    builder.addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder chaining', function() {
    var expected = 'in(donor.gender,"male","female")';
    var builder = PqlUtilService.getBuilder ();

    builder.addTerm ("donor", "gender", "male")
      .addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

});
