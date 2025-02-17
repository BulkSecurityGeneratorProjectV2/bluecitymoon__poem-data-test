(function() {
    'use strict';

    var jhiItemCount = {
        template: '<div class="info">' +
                    'Showing {{(($ctrl.page-1) * 20)==0 ? 1:(($ctrl.page-1) * 20)}} - ' +
                    '{{($ctrl.page * 20) < $ctrl.queryCount ? ($ctrl.page * 20) : $ctrl.queryCount}} ' +
                    'of {{$ctrl.queryCount}} items.' +
                '</div>',
        bindings: {
            page: '<',
            queryCount: '<total'
        }
    };

    angular
        .module('poemdataApp')
        .component('jhiItemCount', jhiItemCount);
})();
