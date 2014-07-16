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

docsApp.controller('DocsCtrl', function ($scope, $sce, $translate) {
  $scope.pages = {
    'API': {
      title: 'API',
      setRecords: function() {$scope.records = docs.api}
    },
    'configuration': {
      title: 'Configuration',
      setRecords: function() {$scope.records = docs.config}
    },
    'attributes': {
      title: 'Attributes',
      setRecords: function() {$scope.records = docs.attributes}
    },
    'processors': {
      title: 'Processor',
      setRecords: function() {$scope.records = docs.processors}
    },
    'layer': {
      title: 'Map Layer',
      setRecords: function() {$scope.records = docs.mapLayers}
    }
  };
  $scope.page = 'API';
  $scope.records = docs.api;
  $scope.select = function (page) {
    $scope.page = page;
    $scope.pages[page].setRecords()
  };
  $scope.renderHtml = function(html){
    return $sce.trustAsHtml($translate(html));
  };
  $scope.select($scope.page);
});

docsApp.controller('DocsCtrl', function ($scope, $sce, $translate, $location) {
  $scope.pages = {
    'API': {
      title: 'API',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.api},
      desc: 'apiPageDesc'
    },
    'configuration': {
      title: 'Configuration',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.config},
      desc: 'configurationPageDesc'
    },
    'attributes': {
      title: 'Attributes',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.attributes},
      desc: 'attributesPageDesc'
    },
    'processors': {
      title: 'Processor',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.processors}   ,
      desc: 'processorsPageDesc'
    },
    'mapLayer': {
      title: 'Map Layer',
      html: 'user-api-part.html',
      setRecords: function() {$scope.records = docs.mapLayers},
      desc: 'mapLayerPageDesc'
    }
  };
  $scope.page = 'API';
  var path = $location.path() || "";
  path = path.substr(1);
  if ($scope.pages[path]) {
    $scope.page = path;
  } else {
    for ($scope.page in $scope.pages) break;
    $location.path('/' + $scope.page);
  }
  $scope.records = docs.api;
  $scope.select = function (page) {
    $scope.page = page;
    $scope.pages[page].setRecords()
  };
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
  $scope.select($scope.page);
});