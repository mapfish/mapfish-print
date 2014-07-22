/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

// Scripts will add the data to this docs variable
var docs = {};

var docsApp = angular.module('docsApp', ['ui.bootstrap', 'pascalprecht.translate']);
docsApp.config(function($translateProvider) {
  $translateProvider.useStaticFilesLoader({
    prefix: 'strings-',
    suffix: '.json'
  });
  $translateProvider.preferredLanguage('en');
});

docsApp.controller('DocsCtrl', function ($scope, $rootScope, $sce, $translate, $location, $anchorScroll, $timeout) {

  $scope.pages = {
    overview: {
      order: 0,
      title: 'tocOverview',
      html: 'overview-part.html',
      setRecords: function() {}
    },
    jasperReports: {
      order: 1,
      title: 'tocJasperReports',
      html: 'jaspert-reports-part.html',
      setRecords: function() {}
    },
    'API': {
      order: 10,
      title: 'tocApiTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.api},
      desc: 'tocApiDesc'
    },
    'configuration': {
      order: 20,
      title: 'tocConfigurationTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.config},
      desc: 'tocConfigurationDesc'
    },
    'attributes': {
      order: 20,
      title: 'tocAttributesTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.attributes},
      desc: 'tocAttributesDesc',
      inputTitle: 'jsonParamTitle'
    },
    'processors': {
      order: 20,
      title: 'tocProcessorTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.processors}   ,
      desc: 'tocProcessorsDesc',
      inputTitle: 'inputParamTitle',
      outputTitle: 'outputParamTitle'
    },
    'mapLayer': {
      order: 20,
      title: 'tocMapLayerTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.mapLayers},
      desc: 'tocMapLayerDesc',
      inputTitle: 'jsonParamTitle'
    },
    'styles': {
      order: 30,
      title: 'tocStyleTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.styles},
      desc: 'tocStyleDesc'
    },
    'outputFormats': {
      order: 30,
      title: 'tocOutputFormatsTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.outputFormats},
      desc: 'tocOutputFormatsDesc'
    },
    'fileLoaders': {
      order: 30,
      title: 'tocFileLoadersTitle',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.fileLoaders},
      desc: 'tocFileLoadersDesc'
    }
  };
  $scope.page = 'overview';
  $scope.records = docs.api;
  $scope.select = function (page) {
    $scope.page = page;
    $scope.pages[page].setRecords()
  };


  $rootScope.$on('$locationChangeSuccess', function(event){
    var page, record, detail;
    var path = $location.path() || "";
    page = path.substr(1);

    if (!$scope.pages[page]) {
      for ($scope.page in $scope.pages) break;
      $location.path('/' + $scope.page);
    }
    $scope.select(page);

    $timeout(function () {
      // wait until page is rendered then scroll to correct element
      console.log($location.hash());
      $anchorScroll();
    }, 500);
  });


  $scope.getTitle = function(record) {
    if (record.translateTitle) {
      return $translate.instant(record.title)
    } else {
      return record.title
    }
  };
  $scope.renderHtml = function(html){
    return $sce.trustAsHtml($translate.instant(html));
  };
  var translatePages = function() {
    var toTranslate = [];
    angular.forEach($scope.pages, function (p){
      toTranslate.push(p.title)
    });

    $translate(toTranslate).then(function (translations) {
      angular.forEach($scope.pages, function (p){
          var titleKey = p.title;
          p.title = translations[titleKey];
      });
    });
  };
  $scope.select($scope.page);

  translatePages();
});

docsApp.filter('sortTableOfContents', function(){
  return function (items) {
    var p, item, filtered = [];
    for (p in items) {
      if (items.hasOwnProperty(p)) {
        item = items[p];
        item.key = p;
        filtered.push(items[p])
      }
    }

    filtered.sort(function(a,b) {
      if (a.order != b.order) {
        return a.order < b.order ? -1 : 1;
      } else {
        return a.title < b.title ? -1 : 1;
      }
    });
    return filtered;
  }
});
docsApp.filter('sortRecords', function($translate){
  return function (items) {

    items.sort(function(a,b) {
      return $translate.instant(a.title) < $translate.instant(b.title)  ? -1 : 1;
    });

    return items;
  }
});

docsApp.hashPathSeparator = '__';
docsApp.controller('RecordCtrl', function ($scope, $location) {
  var title = $scope.record.title;
  $scope.expanded = $location.hash() === title || $location.hash().indexOf(title + docsApp.hashPathSeparator) === 0;

  $scope.setLocationHash = function() {
    $location.hash($scope.record.title);
  };
});



docsApp.controller('DetailCtrl', function ($scope, $location) {
  $scope.setLocationHash = function(id, $event) {
    $event.stopPropagation();
    $location.hash(id);
  };
  $scope.anchorId = function (detail, type) {
    var id = $scope.record.title + docsApp.hashPathSeparator + type + docsApp.hashPathSeparator + detail.title;
    return id.replace(/[:\/\s\[\]\\]/g, '+');
  }
});

