package com.lastwar.ano2193.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "category_instance")
public class CategoryInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private UploadCategory category;

    /** Human-readable label for this specific run (e.g. "Week 24", "Season 1"). */
    private String name;

    /** If true, this instance has no time bounds. startDate and endDate are ignored. */
    private boolean eternal;

    /** Null when eternal = true. */
    private LocalDate startDate;

    /** Null when eternal = true or when this is a single-date instance. */
    private LocalDate endDate;

    /** Short label used inside the instance dropdown (category already selected). */
    public String getInstanceLabel() {
        String n = (name != null && !name.isBlank()) ? name : null;
        String period;
        if (eternal) {
            period = "(eternal)";
        } else if (startDate != null && endDate != null) {
            period = startDate + " to " + endDate;
        } else if (startDate != null) {
            period = startDate.toString();
        } else {
            period = "";
        }
        if (n != null && !period.isEmpty()) return n + " — " + period;
        if (n != null) return n;
        return period.isEmpty() ? "?" : period;
    }

    /** Full label including category name, used in history tables and flash messages. */
    public String getDisplayName() {
        String base = category != null ? category.getName() : "?";
        return base + " — " + getInstanceLabel();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UploadCategory getCategory() { return category; }
    public void setCategory(UploadCategory category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEternal() { return eternal; }
    public void setEternal(boolean eternal) { this.eternal = eternal; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}