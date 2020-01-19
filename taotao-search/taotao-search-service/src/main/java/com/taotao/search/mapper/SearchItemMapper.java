package com.taotao.search.mapper;

import java.util.List;

import com.taotao.common.pojo.SearchItem;

/**
 * 定义Mapper 关联查询三张表 查询出搜索时的商品数据
 * @author aa
 *
 */
public interface SearchItemMapper {

//	查询所有的商品数据
	public List<SearchItem> getSearchItemList();
	
//	根据商品的id查询商品的数据
	public SearchItem getSearchItemById(Long itemId);
}
