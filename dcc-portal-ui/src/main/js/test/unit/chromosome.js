
describe('Test Chromosome Service', function() {
  var Chromosome;
  beforeEach(module('icgc'));

  beforeEach(inject(function (_Chromosome_) {
    window._gaq = [];
    Chromosome = _Chromosome_;
  }));

  it('Test chromosome length', function() {
    var t = Object.keys(Chromosome.get()).length; 
    expect(t).toEqual(25);
  });


  it('Test chromosome validation: char' , function() {
    expect(Chromosome.validate('x')).toEqual(true);
    expect(Chromosome.validate('y')).toEqual(true);
    expect(Chromosome.validate('mt')).toEqual(true);
    expect(Chromosome.validate('23')).toEqual(false);
    expect(Chromosome.validate('abc')).toEqual(false);
  });

  it('Test chromosome validation: char, start', function() {
    expect(Chromosome.validate('1', 200)).toEqual(true);
    expect(Chromosome.validate('1', 999999999)).toEqual(false);

    // Edge case
    expect(Chromosome.validate('3', 0)).toEqual(false);
    expect(Chromosome.validate('3', Chromosome.length('3')+1)).toEqual(false);
  })

  it('Test chromosome validation: char, start, end', function() {
    expect(Chromosome.validate('3', 1, 2)).toEqual(true);

    expect(Chromosome.validate('3', 9999999999, 2)).toEqual(false);
    expect(Chromosome.validate('3', 2, 9999999999)).toEqual(false);
  });



});

