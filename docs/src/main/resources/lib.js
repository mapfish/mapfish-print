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

var docsApp = angular.module('docsApp', ['ui.bootstrap']);

docsApp.controller('DocsCtrl', function ($scope, $http) {
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
  $scope.select($scope.page);
});