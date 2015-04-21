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

  function setPqlInUrl (pql) {
    location.search (paramName, pql);
  }

  it('Testing getRawPql() with empty pql', function() {
     var expectedPql = '';
     var testPql = PqlUtilService.getRawPql();
    
     expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: eq(test,123)', function() {
    var expectedPql = "eq(test,123)";
    setPqlInUrl (expectedPql);

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

  it('Testing getSort() with pql: eq(test,123),sort(age,-gender,+type)', function() {
    var testPql = 'eq(test,123),sort(age,-gender,+type)';
    setPqlInUrl (testPql);

    var expectedSorts = [
    {
      direction: '+',
      field: 'age'
    },
    {
      direction: '-',
      field: 'gender'
    },
    {
      direction: '+',
      field: 'type'
    }];

    var testSorts = PqlUtilService.getSort ();
    
    expect(testSorts).toEqual(expectedSorts);
  });

  it('Testing getSort() with no sort set in pql: eq(test,123)', function() {
    var testPql = 'eq(test,123)';
    setPqlInUrl (testPql);

    var expectedSorts = [];

    var testSorts = PqlUtilService.getSort ();
    
    expect(testSorts).toEqual(expectedSorts);
  });

  it('Testing getLimit() with pql: eq(test,123),sort(age,-gender,+type),limit(1,99)', function() {
    var testPql = 'eq(test,123),sort(age,-gender,+type),limit(1,99)';
    setPqlInUrl (testPql);

    var expectedLimit = {
      from: 1,
      size: 99
    };

    var testLimit = PqlUtilService.getLimit ();
    
    expect(testLimit).toEqual (expectedLimit);
  });

  it('Testing getLimit() with pql: eq(test,123),sort(age,-gender,+type),limit(1)', function() {
    var testPql = 'eq(test,123),sort(age,-gender,+type),limit(1)';
    setPqlInUrl (testPql);

    var expectedLimit = {
      from: 1
    };

    var testLimit = PqlUtilService.getLimit ();
    
    expect(testLimit).toEqual (expectedLimit);
  });

  it('Testing getLimit() with no limit set in pql: eq(test,123)', function() {
    var testPql = 'eq(test,123)';
    setPqlInUrl (testPql);

    var expectedLimit = {};

    var testLimit = PqlUtilService.getLimit ();
    
    expect(testLimit).toEqual (expectedLimit);
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

  it('Testing overwrite() with "donor.gender" set to "unknown" in pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var originalPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    setPqlInUrl (originalPql);

    var expectedQuery = {
      donor: {
        gender: {
          "in": ["unknown"]
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

    PqlUtilService.overwrite ("donor", "gender", "unknown");

    var testQuery = PqlUtilService.getQuery();
    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing mergePqls() with eq(donor.age,123) and in(donor.gender, "male", "female")', function() {
    var pql1 = 'eq(donor.age,123)';
    var pql2 = 'in(donor.gender,"male","female")';

    var expectedPql = 'and(' + pql1 + ',' + pql2 + ')';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with eq(donor.gender,"female") and eq(donor.gender,"male")', function() {
    var pql1 = 'eq(donor.gender,"female")';
    var pql2 = 'eq(donor.gender,"male")';

    var expectedPql = 'eq(donor.gender,"male")';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with eq(donor.gender,"female") and and(eq(donor.gender,"male"), eq(donor.age, 22))', function() {
    var pql1 = 'and(eq(donor.gender,"male"),eq(donor.age,22))';
    var pql2 = 'eq(donor.gender,"female")';

    var expectedPql = 'and(eq(donor.gender,"female"),eq(donor.age,22))';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with and(eq(donor.gender,"female"),in(mutation.foo, 3, 5)) and and(eq(donor.gender,"male"), eq(donor.age, 22))', function() {
    var pql1 = 'and(eq(donor.gender,"female"),in(mutation.foo,3,5))';
    var pql2 = 'and(eq(donor.gender,"male"),eq(donor.age,22))';

    var expectedPql = 'and(eq(donor.gender,"male"),eq(donor.age,22),in(mutation.foo,3,5))';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with no argument', function() {
    var expectedPql = '';
    var mergedPql = PqlUtilService.mergePqls ();

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with one argument: eq(donor.gender,"female")', function() {
    var pql1 = 'eq(donor.gender,"female")';
    var mergedPql = PqlUtilService.mergePqls (pql1);

    expect(mergedPql).toEqual(pql1);
  });


  it('Testing mergeQueries() with eq(donor.age,123) and in(donor.gender, "male", "female")', function() {
    var query1 = {
      donor: {
        age: {
          "in": [123]
        }
      }
    };
    var query2 = {
      donor: {
        gender: {
          'in': ['male', 'female']
        }
      }
    };

    var expectedQuery = {
      donor: {
        age: {
          "in": [123]
        },
        gender: {
          'in': ['male', 'female']
        }
      }
    };

    var mergedQuery = PqlUtilService.mergeQueries (query1, query2);

    expect(mergedQuery).toEqual (expectedQuery);
  });

  it('Testing mergeQueries() with eq(donor.gender,"female") and eq(donor.gender,"male")', function() {
    var query1 = {
      donor: {
        gender: {
          'in': ['female']
        }
      }
    };
    var query2 = {
      donor: {
        gender: {
          'in': ['male']
        }
      }
    };

    var expectedQuery = {
      donor: {
        gender: {
          'in': ['male']
        }
      }
    };

    var mergedQuery = PqlUtilService.mergeQueries (query1, query2);

    expect(mergedQuery).toEqual (expectedQuery);
  });

  it('Testing convertQueryToPql() for pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var testQuery = {
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

    var expectedPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    var testPql = PqlUtilService.convertQueryToPql (testQuery);

    expect(testPql).toEqual (expectedPql);
  });

  it('Testing convertPqlToQuery() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
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

    var testPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    var testQuery = PqlUtilService.convertPqlToQuery (testPql);

    expect(testQuery).toEqual (expectedQuery);
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
