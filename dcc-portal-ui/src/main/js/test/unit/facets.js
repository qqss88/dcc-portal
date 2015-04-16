describe('Test Facet', function() {
  var Facets, httpMock;

  beforeEach(module('icgc'));

  beforeEach(inject(function (_Facets_) {
    window._gaq = [];
    Facets = _Facets_;
  }));

  var idTerms = [
    { term: 'ENSG001', count: 10},
    { term: 'ENSG002', count: 8},
    { term: 'ENSG003', count: 3}
  ];


  it('Test removing terms', function() {
    var result;
    Facets.addTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG001',
    });

    Facets.addTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG003',
    });

    Facets.addTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG002',
    });


    expect(Facets.getActiveTerms({
      type: 'gene', facet: 'id', terms: idTerms
    }).length).toEqual(3);

    Facets.removeTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG001'
    });
    expect(Facets.getActiveTerms({
      type: 'gene', facet: 'id', terms: idTerms
    }).length).toEqual(2);

    Facets.removeTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG002'
    });
    expect(Facets.getActiveTerms({
      type: 'gene', facet: 'id', terms: idTerms
    }).length).toEqual(1);
  });


  it('Test adding terms', function() {
    var result;
    Facets.addTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG001',
    });

    Facets.addTerm({
      type: 'gene',
      facet: 'id',
      term: 'ENSG003',
    });

    result = Facets.getActiveTerms({
      type: 'gene', facet: 'id', terms: idTerms
    });
    expect(result.length).toEqual(2);

    Facets.removeAll();
    result = Facets.getActiveTerms({
      type: 'gene', facet: 'id', terms: idTerms
    });
    expect(result.length).toEqual(0);
  });


});
