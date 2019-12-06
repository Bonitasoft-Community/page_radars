'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('ctrlmonitor', ['googlechart', 'ui.bootstrap','ngSanitize', 'ngModal', 'ngMaterial']);


/* Material : for the autocomplete
 * need
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-animate.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-aria.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-messages.min.js"></script>

  <!-- Angular Material Library -->
  <script src="https://ajax.googleapis.com/ajax/libs/angular_material/1.1.0/angular-material.min.js">
 */



// --------------------------------------------------------------------------
//
// Controler Ping
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('CtrlControler',
	function ( $http, $scope,$sce,$filter ) {

	this.pingdate='';
	this.pinginfo='';
	this.listevents='';
	this.inprogress=false;

	this.refresh = function()
	{

		var self=this;
		self.inprogress=true;
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();

		$http.get( '?page=custompage_workers&action=refresh&t='+d.getTime() )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.photo 		= jsonResult.photo;
						self.listevents			= jsonResult.listevents;

						self.inprogress=false;


				})
				.error( function() {
					alert('an error occure');
					self.inprogress=false;
					});

	}
	this.refresh();




	// -----------------------------------------------------------------------------------------
	//  										Properties
	// -----------------------------------------------------------------------------------------
	this.propsFirstName='';
	this.saveProps = function() {
		var self=this;
		self.inprogress=true;

		var param={ 'firstname': this.propsFirstName };
		var json = encodeURI( angular.toJson( param, false));

		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();

		$http.get( '?page=custompage_workers&action=saveprops&paramjson='+json +'&t='+d.getTime())
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.listevents		= jsonResult.listevents;
						self.inprogress=false;

						alert('Properties saved');
				})
				.error( function() {
					alert('an error occure');
					});
	}

	this.loadProps =function() {
		var self=this;
		self.inprogress=true;

		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();

		$http.get( '?page=custompage_workers&action=loadprops&t='+d.getTime() )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.propsFirstName = jsonResult.firstname;
						self.listevents		= jsonResult.listevents;
						self.inprogress		= false;

				})
				.error( function() {
					alert('an error occure');
					});
	}
	this.loadProps();


	<!-- Manage the event -->
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents );
	}
	<!-- Manage the Modal -->
	this.isshowDialog=false;
	this.openDialog = function()
	{
		this.isshowDialog=true;
	};
	this.closeDialog = function()
	{
		this.isshowDialog=false;
	}

});



})();