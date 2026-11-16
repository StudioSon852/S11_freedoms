package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "sizes")
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="size_id")
    private Long id;

    @Column(name="size_label", nullable=false, unique=true, length=20)
    private String sizeLabel;

    @Column(name="sort_order", nullable=false)
    private Integer sortOrder;

    public Size() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}