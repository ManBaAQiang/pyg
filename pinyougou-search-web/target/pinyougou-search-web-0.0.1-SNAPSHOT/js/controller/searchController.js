app.controller('searchController',function($scope,$location,searchService){
	
	//构建搜索对象的结构		category:商品分类
	$scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sort':'','sortField':''};
	
	//搜索
	$scope.search=function(){
		$scope.searchMap.pageNo= parseInt($scope.searchMap.pageNo);
		searchService.search($scope.searchMap).success(
			function(response){
				$scope.resultMap=response; //数据结构：{"rows":{}}
				
				//构建分页栏
				buildPageLabel();
			}
		)
	};
	
	buildPageLabel=function(){
		$scope.pageLabel=[];
		var firstPage = 1;//开始页
		var lastPage= $scope.resultMap.totalPages;//截止页码
		$scope.firstDot=true;//前面有点
		$scope.lastDot=true;//后边有点
		
		if($scope.resultMap.totalPages>5){//如果页码数量大于5
			if($scope.searchMap.pageNo<=3){//如果当前页码小于等于3 ，显示前5页
				lastPage=5;
				$scope.firstDot=false;//前面没点
			}else if($scope.searchMap.pageNo>= $scope.resultMap.totalPages-2){
				firstPage= $scope.resultMap.totalPages-4;//显示后5页
				$scope.lastDot=false;//后边没点
			}else {//以当前页码为中心的显示5页
				firstPage= $scope.searchMap.pageNo-2;
				lastPage= $scope.searchMap.pageNo+2;
			}
		}else {
			$scope.firstDot=false;//前面无点
			$scope.lastDot=false;//后边无点
		}
		
		//构建页码
		for(var i=firstPage;i<=lastPage;i++){
			$scope.pageLabel.push(i);
		}
	}
	
	//添加搜索项
	$scope.addSearchItem=function(key,value){
		if(key=='category' || key=='brand' || key=='price'){
			//如果用户点击的是分类或者品牌
			$scope.searchMap[key]=value;
		}else {
			//用户点击的是规格
			$scope.searchMap.spec[key] = value;
		}
		
		$scope.search();//查询
	};
	
	//撤销搜索项
	$scope.removeSearchItem=function(key){
		if(key=='category' || key=='brand' || key=='price'){
			$scope.searchMap[key] = "";
		}else {
			delete $scope.searchMap.spec[key];
		}
		
		$scope.search();
	};
	
	//分页查询
	$scope.queryByPage=function(pageNo){
		//页码验证
		if(pageNo<1 || pageNo>$scope.resultMap.totalPages){
			return;
		}
		
		$scope.searchMap.pageNo=pageNo;//
		$scope.search();//查询
	}
	
	//判断当前页为第一页
	$scope.isTopPage=function(){
		if($scope.searchMap.pageNo==1){
			return true;
		}
		return false;
	}
	
	//判断当前页是否未最后一页
	$scope.isEndPage=function(){
		if($scope.searchMap.pageNo==$scope.resultMap.totalPages){
			return true;
		}else{
			return false;
		}
	}
	
	//排序查询
	$scope.sortSearch=function(sortField,sort){
		$scope.searchMap.sortField=sortField;
		$scope.searchMap.sort=sort;
		
		$scope.search();//查询
	}
	
	//判断关键字是否是品牌
	$scope.keywordsIsBrand=function(){
		for(var i=0;i< $scope.resultMap.brandList.length;i++){
			if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>=0){//如果包含
				return true;
			}
		}
		return false;
	}
	
	//加载查询字符串
	$scope.loadkeywords=function(){
		$scope.searchMap.keywords= $location.search()['keywords'];
		$scope.search();
	}
	
})