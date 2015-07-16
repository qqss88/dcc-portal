/*
 * Test adding and removing sets
 */
describe('Test SetService', function() {
  var SetService, httpMock;

  beforeEach(module('icgc'));

  beforeEach(inject(function ($httpBackend, $q, $rootScope, _SetService_, _API_) {
    window._gaq = [];
    httpMock = $httpBackend;
    SetService = _SetService_;
    API = _API_;

    // Not sure why these are needed
    httpMock.when('GET', API.BASE_URL + '/releases/current').respond({});
    httpMock.when('GET', 'views/home.html').respond({});

    httpMock.when('POST', API.BASE_URL + '/entityset').respond({
      id: 'uu-id-1',
      name: 'regular set',
      type: 'donor',
      count: 10
    });
    httpMock.when('POST', API.BASE_URL + '/entityset/union').respond({
      id: 'uu-id-2',
      name: 'derived set',
      type: 'donor',
      count: 10
    });
  }));

  it('Test adding new set', function() {
    expect(SetService.getAll().length).toEqual(0);
    var promise = SetService.addSet('donor', {filters:{}});
    httpMock.flush();

    // Flush SetService
    SetService.initService();
    expect(SetService.getAll().length).toEqual(1);
    expect(SetService.getAll()[0].name).toEqual('regular set');
  });

  it('Test adding derived set', function() {
    expect(SetService.getAll().length).toEqual(1);
    var promise = SetService.addDerivedSet('donor', {filters:{}});
    httpMock.flush();

    // Flush SetService
    SetService.initService();
    expect(SetService.getAll().length).toEqual(2);
    expect(SetService.getAll()[0].name).toEqual('derived set');
    expect(SetService.getAll()[1].name).toEqual('regular set');
  });


  it('Test removing set', function() {
    expect(SetService.getAll().length).toEqual(2);
    SetService.remove('uu-id-2');
    expect(SetService.getAll().length).toEqual(1);
    SetService.remove('uu-id-2');
    expect(SetService.getAll().length).toEqual(1);
    SetService.remove('uu-id-1');
    expect(SetService.getAll().length).toEqual(0);
  });


});
