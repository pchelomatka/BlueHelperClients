package com.example.bluehelperclients;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Vector {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("building_id")
    @Expose
    private String buildingId;
    @SerializedName("start_point")
    @Expose
    private String startPoint;
    @SerializedName("end_point")
    @Expose
    private String endPoint;
    @SerializedName("distance")
    @Expose
    private String distance;
    @SerializedName("direction")
    @Expose
    private String direction;
    @SerializedName("edited_by")
    @Expose
    private String editedBy;
    @SerializedName("last_update")
    @Expose
    private String lastUpdate;

    public Vector(String id, String buildingId, String startPoint, String endPoint, String distance, String direction, String editedBy, String lastUpdate) {
        this.id = id;
        this.buildingId = buildingId;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.distance = distance;
        this.direction = direction;
        this.editedBy = editedBy;
        this.lastUpdate = lastUpdate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
