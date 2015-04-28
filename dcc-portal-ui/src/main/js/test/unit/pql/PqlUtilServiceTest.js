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

  it('Testing reset() with pql: eq(test,123)', function() {
    var originalPql = "eq(test,123)";
    setPqlInUrl (originalPql);

    PqlUtilService.reset();
    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual('');
  });

  it('Testing getRawPql() with pql: eq(donor.gender,"male")', function() {
    var category = "donor";
    var facet = "gender";
    var term = "male";
    var expectedPql = 'select(*),eq(' + category + '.' + facet + ',' + '"' + term + '"' + ')';

    PqlUtilService.addTerm (category, facet, term);

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: in(donor.gender,"male", "female")', function() {
    var category = "donor";
    var facet = "gender";
    var term1 = "male";
    var term2 = "female";
    var expectedPql = 'select(*),in(' + category + '.' + facet + ',' + '"' + term1 + '"'
      + ',' + '"' + term2 + '"' + ')';

    PqlUtilService.addTerm (category, facet, term1);
    PqlUtilService.addTerm (category, facet, term2);

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing addTerms() with pql: in(donor.gender,"male", "female", "unknown")', function() {
    var expectedPql = 'select(*),in(donor.gender,"male","female","unknown")';

    PqlUtilService.addTerms ('donor', 'gender', ['male', 'female', 'unknown']);

    var testPql = PqlUtilService.getRawPql();

    expect(testPql).toEqual(expectedPql);
  });

  it('Testing getRawPql() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22))', function() {
    var category = "donor";
    var facet = "gender";
    var term1 = "male";
    var term2 = "female";
    var expectedPql = 'select(*),and(in(donor.gender,"male","female"),eq(donor.age,22))';

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
    var expectedPql = 'select(*),and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';

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
    var testPql = 'eq(donor.test,123),sort(age,-gender,+type),limit(1,99)';
    setPqlInUrl (testPql);

    var expectedLimit = {
      from: 1,
      size: 99
    };

    var testLimit = PqlUtilService.getLimit ();

    expect(testLimit).toEqual (expectedLimit);
  });

  it('Testing getLimit() with pql: eq(test,123),sort(age,-gender,+type),limit(1)', function() {
    var testPql = 'eq(donor.test,123),sort(age,-gender,+type),limit(1)';
    setPqlInUrl (testPql);

    var expectedLimit = {
      size: 1
    };

    var testLimit = PqlUtilService.getLimit ();

    expect(testLimit).toEqual (expectedLimit);
  });

  it('Testing getLimit() with no limit set in pql: eq(donor.test,123)', function() {
    var testPql = 'eq(donor.test,123)';
    setPqlInUrl (testPql);

    var expectedLimit = {};

    var testLimit = PqlUtilService.getLimit ();

    expect(testLimit).toEqual (expectedLimit);
  });

  it('Testing includesFacets() in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),facets(*),' + originalPql;

    PqlUtilService.includesFacets ();

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing includesConsequences() in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),select(consequences),' + originalPql;

    PqlUtilService.includesConsequences ();

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing setLimit() in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),' + originalPql + ',limit(1,99)';

    var limit = {from: 1, size: 99};
    PqlUtilService.setLimit (limit);

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing limitFromSize() in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),' + originalPql + ',limit(99,88)';

    PqlUtilService.limitFromSize (99, 88);

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing limitSize() in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),' + originalPql + ',limit(77)';

    PqlUtilService.limitSize (77);

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing setLimit() with "size" set to a float (2.51) in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),' + originalPql + ',limit(2)';

    var limit = {size: 2.51};
    PqlUtilService.setLimit (limit);

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing setSort() in pql: eq(donor.test,123)', function() {
    var originalPql = 'eq(donor.test,123)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),' + originalPql + ',sort(+donor.age,-donor.foo)';

    var sort = [{field: 'donor.age', direction: '+'}, {field: 'donor.foo', direction: '-'}];
    PqlUtilService.setSort (sort);

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing sortAsc() in pql: eq(donor.test,123),sort(-donor.foo)', function() {
    var originalPql = 'eq(donor.test,123),sort(-donor.foo)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),eq(donor.test,123),sort(-donor.foo,+donor.age)';

    PqlUtilService.sortAsc ('donor.age');

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing sortDesc() in pql: eq(donor.test,123),sort(donor.foo)', function() {
    var originalPql = 'eq(donor.test,123),sort(donor.foo)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),eq(donor.test,123),sort(+donor.foo,-donor.age)';

    PqlUtilService.sortDesc ('donor.age');

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing removeSort() in pql: eq(donor.test,123),sort(-donor.foo,+donor.bar)', function() {
    var originalPql = 'eq(donor.test,123),sort(-donor.foo,+donor.bar)';
    setPqlInUrl (originalPql);

    var expectedPql = 'select(*),eq(donor.test,123),sort(+donor.bar)';

    PqlUtilService.removeSort ('donor.foo');

    expect(PqlUtilService.getRawPql()).toEqual (expectedPql);
  });

  it('Testing getFilters() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
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

    var testQuery = PqlUtilService.getFilters();

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

    var testQuery = PqlUtilService.getFilters();

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

    var testQuery = PqlUtilService.getFilters();

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

    var testQuery = PqlUtilService.getFilters();

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

    var testQuery = PqlUtilService.getFilters();
    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing overwrite() with "donor.gender" set to ["unknown", "male", "alien"] in pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var originalPql = 'and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    setPqlInUrl (originalPql);

    var expectedQuery = {
      donor: {
        gender: {
          "in": ["unknown", "male", "alien"]
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

    PqlUtilService.overwrite ("donor", "gender", ["unknown", "male", "alien"]);

    var testQuery = PqlUtilService.getFilters();
    expect(testQuery).toEqual(expectedQuery);
  });

  it('Testing mergePqls() with eq(donor.age,123) and in(donor.gender, "male", "female")', function() {
    var pql1 = 'eq(donor.age,123)';
    var pql2 = 'in(donor.gender,"male","female")';

    var expectedPql = 'select(*),and(' + pql1 + ',' + pql2 + ')';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with eq(donor.gender,"female") and eq(donor.gender,"male")', function() {
    var pql1 = 'eq(donor.gender,"female")';
    var pql2 = 'eq(donor.gender,"male")';

    var expectedPql = 'select(*),eq(donor.gender,"male")';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with eq(donor.gender,"female") and and(eq(donor.gender,"male"), eq(donor.age, 22))', function() {
    var pql1 = 'and(eq(donor.gender,"male"),eq(donor.age,22))';
    var pql2 = 'eq(donor.gender,"female")';

    var expectedPql = 'select(*),and(eq(donor.gender,"female"),eq(donor.age,22))';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with and(eq(donor.gender,"female"),in(mutation.foo, 3, 5)) and and(eq(donor.gender,"male"), eq(donor.age, 22))', function() {
    var pql1 = 'and(eq(donor.gender,"female"),in(mutation.foo,3,5))';
    var pql2 = 'and(eq(donor.gender,"male"),eq(donor.age,22))';

    var expectedPql = 'select(*),and(eq(donor.gender,"male"),eq(donor.age,22),in(mutation.foo,3,5))';

    var mergedPql = PqlUtilService.mergePqls (pql1, pql2);

    expect(mergedPql).toEqual(expectedPql);
  });

  it('Testing mergePqls() with and(eq(donor.gender,"female"),in(mutation.foo, 3, 5)) and and(eq(donor.gender,"male"), eq(donor.age, 22))', function() {
    var pql1 = 'select(*),and(eq(donor.gender,"female"),in(mutation.foo,3,5)),sort(a, -b)';
    var pql2 = 'select(*),and(eq(donor.gender,"male"),eq(donor.age,22)),limit(88)';

    var expectedPql = 'select(*),and(eq(donor.gender,"male"),eq(donor.age,22),in(mutation.foo,3,5)),sort(+a,-b),limit(88)';

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

  it('Testing mergeQueries() with no argument', function() {
    var expectedQuery = {};
    var mergedQuery = PqlUtilService.mergeQueries ();

    expect(mergedQuery).toEqual (expectedQuery);
  });

  it('Testing mergeQueries() with one argument', function() {
    var expectedQuery = {
      donor: {
        gender: {
          'in': ['male']
        }
      }
    };
    var mergedQuery = PqlUtilService.mergeQueries (expectedQuery);

    expect(mergedQuery).toEqual (expectedQuery);
  });

  it('Testing convertQueryToPql() for pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var testQuery = {
      params: {
        selectAll: true,
        limit: {},
        sort: []
      },
      filters: {
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
      }
    };

    var expectedPql = 'select(*),and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    var testPql = PqlUtilService.convertQueryToPql (testQuery);

    expect(testPql).toEqual (expectedPql);
  });

  it('Testing convertPqlToQuery() with pql: and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))', function() {
    var expectedQuery = {
      params: {
        selectAll: true,
        customSelects: [],
        facets: false,
        limit: {},
        sort: []
      },
      filters: {
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
      }
    };

    var testPql = 'select(*),and(in(donor.gender,"male","female"),eq(donor.age,22),eq(mutation.foo,"bar"))';
    var testQuery = PqlUtilService.convertPqlToQuery (testPql);

    expect(testQuery).toEqual (expectedQuery);
  });

  it('Testing Builder.build()', function() {
    var expected = 'select(*),in(donor.gender,"male","female")';
    var builder = PqlUtilService.getBuilder ();

    builder.addTerm ("donor", "gender", "male");
    builder.addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder chaining', function() {
    var expected = 'select(*),in(donor.gender,"male","female")';
    var builder = PqlUtilService.getBuilder ();

    builder.addTerm ("donor", "gender", "male")
      .addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder initialized with a PQL', function() {
    var initialPql = 'select(*),and(eq(donor.gender,"male"),eq(donor.id,"some-uuid"))';
    var builder = PqlUtilService.getBuilder (initialPql);

    var expected = 'select(*),facets(*),and(in(donor.gender,"male","female"),in(donor.id,"uuid1","uuid3"),in(donor.foo,123,789)),sort(-donor.age),limit(20,30)';

    builder.removeTerm ('removing', 'something', 'non-existent')
      .includesFacets()
      .overwrite ('donor', 'id', ['uuid1', 'uuid2', 'uuid3'])
      .setSort ([{field: 'donor.age', direction: '-'}])
      .addTerm ('donor', 'gender', 'female')
      .addTerm ('donor', 'foo', 123)
      .addTerm ('remove', 'this', 'soon')
      .setLimit ({from: 20, size:30})
      .removeTerm ('donor', 'id', 'uuid2')
      .removeFacet ('remove', 'this')
      .addTerm ('donor', 'foo', 789)
      ;

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder.reset()', function() {
    var initialPql = 'select(*),and(eq(donor.gender,"male"),eq(donor.id,"some-uuid"))';
    var builder = PqlUtilService.getBuilder (initialPql);

    var expected = 'select(*),and(eq(donor.gender,"male"),eq(donor.id,"some-uuid"),eq(donor.age,23)),limit(30)';

    builder.removeTerm ('removing', 'something', 'non-existent')
      .includesFacets()
      .overwrite ('donor', 'id', ['uuid1', 'uuid2', 'uuid3'])
      .setSort ([{field: 'donor.age', direction: '-'}])
      .addTerm ('donor', 'gender', 'female')
      .addTerm ('donor', 'foo', 123)
      .addTerm ('remove', 'this', 'soon')
      .setLimit ({from: 20, size:30})
      .removeTerm ('donor', 'id', 'uuid2')
      .removeFacet ('remove', 'this')
      .addTerm ('donor', 'foo', 789)
      .reset()
      .setLimit ({size: 30})
      .addTerm ('donor', 'age', 23)
      ;

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder.addTerms()', function() {
    var initialPql = 'select(*),and(eq(donor.gender,"male"),eq(donor.id,"some-uuid"))';
    var builder = PqlUtilService.getBuilder (initialPql);

    var expected = 'select(*),facets(*),in(donor.foo,789,333,1)';

    builder.removeTerm ('removing', 'something', 'non-existent')
      .removeFacet ('donor', 'id')
      .includesFacets()
      .removeFacet ('donor', 'gender')
      .includesFacets()
      .addTerms ('donor', 'foo', [789, 333, 1]);
      ;

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder.includesFacets()', function() {
    var expected = 'select(*),facets(*),in(donor.gender,"male","female")';
    var builder = PqlUtilService.getBuilder ();

    builder.addTerm ("donor", "gender", "male")
      .includesFacets()
      .addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder.includesConsequences()', function() {
    var expected = 'select(*),select(consequences),in(donor.gender,"male","female")';
    var builder = PqlUtilService.getBuilder ();

    builder.addTerm ("donor", "gender", "male")
      .includesConsequences()
      .addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder.setLimit()', function() {
    var expected = 'select(*),facets(*),in(donor.gender,"male","female"),limit(1,99)';
    var limit = {from: 1, size: 99};

    var builder = PqlUtilService.getBuilder ();
    builder.addTerm ("donor", "gender", "male")
      .includesFacets()
      .setLimit (limit)
      .addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

  it('Testing Builder.setSort()', function() {
    var expected = 'select(*),facets(*),in(donor.gender,"male","female"),sort(+donor.age,-donor.foo),limit(1,99)';
    var limit = {from: 1, size: 99};
    var sort = [{field: 'donor.age', direction: '+'}, {field: 'donor.foo', direction: '-'}];

    var builder = PqlUtilService.getBuilder ();
    builder.setSort (sort)
      .addTerm ("donor", "gender", "male")
      .includesFacets()
      .setLimit (limit)
      .addTerm ("donor", "gender", "female");

    expect(builder.build()).toEqual(expected);
  });

});
