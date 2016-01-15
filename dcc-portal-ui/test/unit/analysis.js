/**
 * Test adding and removing analysis
 */
describe('Test AnalysisService', function() {

  var AnalysisService, httpMock;
  beforeEach(module('icgc'));

  beforeEach(inject(function ($httpBackend, $q, $rootScope, _AnalysisService_) {
    window._gaq = [];
    httpMock = $httpBackend;
    AnalysisService = _AnalysisService_;

    // Not sure why these are needed
    httpMock.when('GET', '/api/v1/releases/current').respond({});
    httpMock.when('GET', 'views/home.html').respond({});
  }));


  it('Adding analyses', function() {
    AnalysisService.addAnalysis({
      id: 'analysis-1',
      inputCount: 3,
      type: 'phenotype'
    }, 'phenotype');

    AnalysisService.addAnalysis({
      id: 'analysis-2',
      inputCount: 3,
      type: 'union',
    }, 'union');

    AnalysisService.addAnalysis({
      id: 'analysis-3',
      params: {
        universe: 'REACTOME', 
        maxGeneCount: 100,
      },
      type: 'enrichment'
    }, 'enrichment');

    expect(AnalysisService.getAll().length).toEqual(3);
  });

  it('Adding duplicated analysis', function() {
    AnalysisService.addAnalysis({
      id: 'analysis-1',
      inputCount: 3,
      type: 'phenotype'
    }, 'phenotype');

    expect(AnalysisService.getAll().length).toEqual(3);
  });

  it('Removing analyses', function() {
    AnalysisService.remove('analysis-3');

    expect(AnalysisService.getAll().length).toEqual(2);
    var t = _.filter(AnalysisService.getAll(), function(analysis) {
      return analysis.id === 'analysis-3';
    }).length;
    expect(t).toEqual(0);
  });

  it('Removing all', function() {
    AnalysisService.removeAll();
    expect(AnalysisService.getAll().length).toEqual(0);
  });


});
