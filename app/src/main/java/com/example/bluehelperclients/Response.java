package com.example.bluehelperclients;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Response {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("device_id")
    @Expose
    private String deviceId;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("building_id")
    @Expose
    private String buildingId;
    @SerializedName("edited_by")
    @Expose
    private String editedBy;
    @SerializedName("last_update")
    @Expose
    private String lastUpdate;
    @SerializedName("vectors")
    @Expose
    private List<Vector> vectors = null;

    public Response(String id, String deviceId, String title, String buildingId, String editedBy, String lastUpdate, List<Vector> vectors) {
        this.id = id;
        this.deviceId = deviceId;
        this.title = title;
        this.buildingId = buildingId;
        this.editedBy = editedBy;
        this.lastUpdate = lastUpdate;
        this.vectors = vectors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
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

    public List<Vector> getVectors() {
        return vectors;
    }

    public void setVectors(List<Vector> vectors) {
        this.vectors = vectors;
    }
}
