app.controller('indexController',function($scope,$controller,loginService){
	//读取当前的登陆人
	$scope.showLoginName=function(){
		loginService.loginName().success(
			function(response) {
				$scope.loginName=response.loginName;
			}
		);
	}
	
})