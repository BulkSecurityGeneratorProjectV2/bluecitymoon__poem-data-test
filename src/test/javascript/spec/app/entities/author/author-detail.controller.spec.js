'use strict';

describe('Controller Tests', function() {

    describe('Author Management Detail Controller', function() {
        var $scope, $rootScope;
        var MockEntity, MockAuthor, MockPoem;
        var createController;

        beforeEach(inject(function($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            MockEntity = jasmine.createSpy('MockEntity');
            MockAuthor = jasmine.createSpy('MockAuthor');
            MockPoem = jasmine.createSpy('MockPoem');
            

            var locals = {
                '$scope': $scope,
                '$rootScope': $rootScope,
                'entity': MockEntity ,
                'Author': MockAuthor,
                'Poem': MockPoem
            };
            createController = function() {
                $injector.get('$controller')("AuthorDetailController", locals);
            };
        }));


        describe('Root Scope Listening', function() {
            it('Unregisters root scope listener upon scope destruction', function() {
                var eventType = 'poemdataApp:authorUpdate';

                createController();
                expect($rootScope.$$listenerCount[eventType]).toEqual(1);

                $scope.$destroy();
                expect($rootScope.$$listenerCount[eventType]).toBeUndefined();
            });
        });
    });

});
