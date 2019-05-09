package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;

@Service(timeout=5000) //服务的提供者-- /Dubbox默认超时时间为1秒，Dubbox调用采用的协议是：RPC(远程过程调用协议)
public class ItemSearchServiceImpl implements ItemSearchService {

	@Autowired
	private SolrTemplate solrTemplate;
	
	@Autowired
	private RedisTemplate redisTemplate;
	
	@Override
	public Map<String, Object> search(Map searchMap) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		String keywords = (String) searchMap.get("keywords");
		searchMap.put("keywords", keywords.replace(" ", ""));//关键字去掉空格
		
		map.putAll(searchList(searchMap));//查询列表（高亮显示）
		//根据关键字查询商品分类
		List<String> categoryList = searchCategoryList(searchMap);
		map.put("categoryList", categoryList);
		
		//查询品牌和规格列表
		String categoryName = (String) searchMap.get("category");
		if(!"".equals(categoryName)){//如果存在分类名称
			map.putAll(searchBrandAndSpecList(categoryName));
		}else {
			if(categoryList.size() > 0){//如果没有分类名称，则按照第一个查询
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}
		}
		
		return map;
//		SimpleQuery query = new SimpleQuery("*:*");
		/*Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		ScoredPage<TbItem> page = solrTemplate.queryForPage(query, TbItem.class);*/
//		map.put("rows", page.getContent());
	}
	
	private Map searchList(Map searchMap){
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		HighlightQuery query = new SimpleHighlightQuery();
		HighlightOptions option = new HighlightOptions().addField("item_title");//设置高亮显示的域
		//构建高亮显示对象
		option.setSimplePrefix("<em style='color:red'>");//设置前缀
		option.setSimplePostfix("</em>");//设置后缀
		
		query.setHighlightOptions(option);
		
		//按照关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		
		//按照商品分类过滤
		if(!"".equals(searchMap.get("category"))){
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
			filterQuery.addCriteria(filterCriteria );
			query.addFilterQuery(filterQuery );//过滤查询
		}
		
		//按照品牌过滤
		if(!"".equals(searchMap.get("brand"))){
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
			filterQuery.addCriteria(filterCriteria );
			query.addFilterQuery(filterQuery );//过滤查询
		}
		
		//按照规格过滤
		if(searchMap.get("spec") != null){
			Map<String,String> specMap = (Map<String, String>) searchMap.get("spec");
			
			for (String key : specMap.keySet()) {
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_spec_"+key).is(searchMap.get(key));
				filterQuery.addCriteria(filterCriteria );
				query.addFilterQuery(filterQuery );//过滤查询
			}
			
		}
		//按照价格筛选
		if(!"".equals(searchMap.get("price"))){
			String[] price = ((String) searchMap.get("price")).split("-");
			if(!price[0].equals("0")){
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
			if(!price[1].equals("*")){//如果最高价格不等于*
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}
		
		//分页查询
		Integer pageNo = (Integer) searchMap.get("pageNo");
		if(pageNo == null){
			pageNo=1;
		}
		Integer pageSize = (Integer) searchMap.get("pageSize");
		if(pageSize == null){
			pageSize=20;
		}
		
		query.setOffset((pageNo-1)*pageSize);//起始索引
		query.setRows(pageSize);//设置每页显示记录数
		
		//按价格排序
		String sortValue = (String) searchMap.get("sort");
		String sortField = (String) searchMap.get("sortField");
		
		if(sortValue != null && !sortValue.equals("")){
			if(sortValue.equals("ASC")){
				Sort sort = new Sort(Sort.Direction.ASC,"item_"+sortField);
				query.addSort(sort);
			}
			if(sortValue.equals("DESC")){
				Sort sort = new Sort(Sort.Direction.DESC,"item_"+sortField);
				query.addSort(sort);
			}
		}
		
		Sort sort = new Sort(Sort.Direction.ASC,"item_price");
		
		query.addSort(sort);
		
		//高亮结果集
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		//高亮入口集合
		List<HighlightEntry<TbItem>> highlighted = page.getHighlighted();
		
		for (HighlightEntry<TbItem> h : highlighted) {
			TbItem tbItem = h.getEntity();//获取原实体类
			//获取高亮列表（高亮域的个数）
			List<Highlight> list = h.getHighlights();
			if(list.size() > 0 && list.get(0).getSnipplets().size() > 0){
				tbItem.setTitle(list.get(0).getSnipplets().get(0));//设置高亮显示结果
			}
			
		}
		
		map.put("rows", page.getContent());
		map.put("totalPages", page.getTotalPages());//总页数
		map.put("total", page.getTotalElements());//总记录数
		return map;
	}

	private List searchCategoryList(Map searchMap){
		List<String> list = new ArrayList<String>();
		
		Query query = new SimpleQuery("*:*");
		//按照关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));//where
		query.addCriteria(criteria);
		//设置分组选项
		GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");//相当于group by
		query.setGroupOptions(groupOptions );
		//获取分组页
		//根据solr核心api（solrTemplate），顺藤摸瓜往下写
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
		//获取分组结果对象
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		//获取分组入口页
		Page<GroupEntry<TbItem>> group = groupResult.getGroupEntries();
		//获取分组入口集合
		List<GroupEntry<TbItem>> content = group.getContent();
		for (GroupEntry<TbItem> entry : content) {
			list.add(entry.getGroupValue());//将分组结果添加到list中。
		}
		
		return list;
	}
	
	//查询品牌和规格列表
	private Map searchBrandAndSpecList(String category){
		Map map=new HashMap();
		
		Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);//获取模板id
		if(typeId != null){
			//根据模板id查询品牌列表
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
			map.put("brandList", brandList);//添加商品列表
			//根据模板id查询规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
			map.put("specList", specList);
		}
		
		return map;
	}

	@Override
	public void importList(List list) {
		solrTemplate.saveBeans(list);
		solrTemplate.commit();
	}

	//goodsIdList--SPU的id
	@Override
	public void deleteByGoodsIds(List goodsIdList) {
		System.out.println("删除商品ID"+goodsIdList);
		
		Query query = new SimpleQuery("*:*");
		Criteria criteria = new Criteria("item_goodsid").in(goodsIdList);
		query.addCriteria(criteria);
		solrTemplate.delete(query);
		solrTemplate.commit();
	}
}
