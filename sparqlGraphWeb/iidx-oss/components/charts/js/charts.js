define([
  'charts/area',
  'charts/area-stacked',
  'charts/bar',
  'charts/bar-stacked',
  'charts/column',
  'charts/column-stacked',
  'charts/donut',
  'charts/line',
  'charts/pie',
  'charts/scatter',
  'charts/spiderweb',
  'charts/spiderweb-comparison',
  'charts/spiderweb-tiny',
  'charts/stock'
], function (area, areaStacked, bar, barStacked, column, columnStacked, donut, line, pie, scatter, spiderweb, spiderwebComparison, spiderwebTiny, stock) {
  'use strict';
  return {
    area: area,
    areaStacked: areaStacked,
    bar: bar,
    barStacked: barStacked,
    column: column,
    columnStacked: columnStacked,
    donut: donut,
    line: line,
    pie: pie,
    scatter: scatter,
    spiderweb: spiderweb,
    spiderwebComparison: spiderwebComparison,
    spiderwebTiny: spiderwebTiny,
    stock: stock
  };
});
