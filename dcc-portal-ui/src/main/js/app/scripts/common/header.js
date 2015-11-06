
(function() {
  'use strict';

  var moduleNamespace = 'app.common.header';
  var controllersNamespace = moduleNamespace + '.controllers';
  var controllerName = 'MainHeaderController';

  angular.module (moduleNamespace, [controllersNamespace]);
  angular.module (controllersNamespace, [])
    .controller (controllerName, function (RouteInfoService) {
    function styleClass (name) {
      return 't_nav__items__item__' + name;
    }

    var items = _.map (['home', 'projects', 'advancedSearch', 'dataAnalysis', 'dataReleases', 'dataRepositories'],
      RouteInfoService.get);
    var itemAttributes = [{
        icon: 'icon-home',
        styleClass: styleClass ('home')
      }, {
        icon: 'icon-list',
        styleClass: styleClass ('projects')
      }, {
        icon: 'icon-search',
        styleClass: styleClass ('advanced')
      }, {
        icon: 'icon-beaker',
        styleClass: styleClass ('analysis')
      }, {
        icon: 'icon-database',
        styleClass: styleClass ('download')
      }, {
        icon: 'icon-download-cloud',
        styleClass: styleClass ('data_repositories')
      }];
    /*
     * Since _.zipWith was introduced in lodash 3.8.0 and we're still using 3.7.x, this is
     * my poor man's implementation. Once we upgrade lodash, all this can be reduced to one line:
     * var menuItems = _.zipWith (items, itemAttributes, _.assign);
     */
    var menuItems = _.map (_.zip (items, itemAttributes), function (itemPair) {
      return _.reduce (itemPair, function (result, item) {
        return _.assign (result, item);
      }, {});
    });

    this.menuItems = menuItems;
  });

})();
