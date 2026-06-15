package com.lastwar.ano2193.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "ranking_entries")
public class RankingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rank;
    private String playerName;
    private String allianceTag;
    private Long power;
    private Long kills;
    private String category;
    private String sourcePhotoPath;
    private String submittedBy;
    private LocalDateTime capturedAt;

    @ElementCollection
    @CollectionTable(name = "ranking_metadata", joinColumns = @JoinColumn(name = "entry_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getAllianceTag() { return allianceTag; }
    public void setAllianceTag(String allianceTag) { this.allianceTag = allianceTag; }

    public Long getPower() { return power; }
    public void setPower(Long power) { this.power = power; }

    public Long getKills() { return kills; }
    public void setKills(Long kills) { this.kills = kills; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSourcePhotoPath() { return sourcePhotoPath; }
    public void setSourcePhotoPath(String sourcePhotoPath) { this.sourcePhotoPath = sourcePhotoPath; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
