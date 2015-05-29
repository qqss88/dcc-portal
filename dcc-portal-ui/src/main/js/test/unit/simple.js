/*
describe('Test LocationService', function() {
  var LocationService;
  beforeEach(module('icgc'));

  beforeEach(inject(function (_LocationService_) {
    LocationService = _LocationService_;
  }));

  it('Test set filters', function() {
    LocationService.setFilters({donor:{id:{is:['DO00000']}}});
    var f = LocationService.mergeIntoFilters({gene:{id:{is:['ENSG00000']}}});

    expect(f).toEqual({
      donor: {id:{is:['DO00000']}},
      gene: {id:{is:['ENSG00000']}},
    });

    LocationService.clear();
    f = LocationService.filters();
    expect(f).toEqual({});
  });
});


describe('Test DefinitionService', function() {
  beforeEach(module('icgc'));
  var httpBackend, DefinitionService;

  beforeEach(inject(function (_DefinitionService_, $httpBackend) {
    DefinitionService = _DefinitionService_;
    httpBackend = $httpBackend;
  }));

  it('Check definition of SSM', function() {
    var ssm = DefinitionService.getDefinitions()['SSM'];
    expect(ssm).toEqual('Simple Somatic Mutation');
  });

});
*/
