package com.mapxus.positioningsample;


import com.mapxus.services.BuildingSearch;
import com.mapxus.services.model.DetailSearchOption;

public class PositionViewModel {
    /**
     * Search detail building info by building id
     * @param buildingId
     * @param buildingSearchResultListener
     */
    public void searchBuildingById(String buildingId, BuildingSearch.BuildingSearchResultListener buildingSearchResultListener) {
        BuildingSearch buildingSearch = BuildingSearch.newInstance();
        buildingSearch.setBuildingSearchResultListener(buildingSearchResultListener);
        DetailSearchOption detailSearchOption = new DetailSearchOption();
        detailSearchOption.id(buildingId);
        buildingSearch.searchBuildingDetail(detailSearchOption);
    }
}
