describe('Test SetOperationService', function() {
  var SetOperationService;

  beforeEach(module('icgc'));

  beforeEach(inject(function ($httpBackend, $q, $rootScope, _SetOperationService_) {
    window._gaq = [];
    SetOperationService= _SetOperationService_;
  }));

  it('Test set equal operation', function() {
     expect(SetOperationService.isEqual(['a', 'b'], ['a', 'b'])).toEqual(true);
     expect(SetOperationService.isEqual(['a', 'b'], ['b', 'a'])).toEqual(true);
     expect(SetOperationService.isEqual(['a', 'b'], ['a', 'a'])).toEqual(false);
     expect(SetOperationService.isEqual(['a', 'b'], ['a', 'b', 'c'])).toEqual(false);
  });

  it('Test set operation alias', function() {
     expect(SetOperationService.getSetShortHand('a', ['b', 'a', 'c'])).toEqual('S2');
     expect(SetOperationService.getSetShortHand('a', ['a', 'b', 'c'])).toEqual('S1');
  });

});

