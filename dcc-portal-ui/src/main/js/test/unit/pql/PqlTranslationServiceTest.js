// Test suite for PqlTranslateService

describe('Testing PqlTranslationService', function() {
  var PqlTranslationService;

  beforeEach(module('icgc'));

  beforeEach(inject(function (_PqlTranslationService_) {
    window._gaq = [];
    PqlTranslationService = _PqlTranslationService_;
  }));

  it('Testing fromPql()', function() {
     var sourcePql = "eq(test, 123)";
     var expectedJson = [{op: "eq", field: "test", values: [123]}];
    
     var tree = PqlTranslationService.fromPql(sourcePql);
     expect(tree).toEqual(expectedJson);
  });

  it('Testing toPql()', function() {
    var sourceJson = [{op: "eq", field: "test", values: [123]}];
    var expectedPql = "eq(test,123)";

    var pql = PqlTranslationService.toPql (sourceJson);
    expect (pql).toEqual(expectedPql);
  });

  it('Testing fromPql()', function() {
     var sourcePql = "eq(test, '123')";
     var expectedJson = [{op: "eq", field: "test", values: ['123']}];
    
     var tree = PqlTranslationService.fromPql(sourcePql);
     expect(tree).toEqual(expectedJson);
  });

  it('Testing fromPql() for pql: eq(donor.gender,"male")', function() {
     var sourcePql = 'eq(donor.gender,"male")';
     var expectedJson = [{op: "eq", field: "donor.gender", values: ['male']}];
    
     var tree = PqlTranslationService.fromPql(sourcePql);
     expect(tree).toEqual(expectedJson);
  });

  it('Testing toPql()', function() {
    var sourceJson = [{op: "eq", field: "test", values: ['123']}];
    var expectedPql = 'eq(test,"123")';

    var pql = PqlTranslationService.toPql (sourceJson);
    expect (pql).toEqual(expectedPql);
  });

});

