package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="history_id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    private CustomerOrder order;

    @Column(name="old_status", length=30)
    private String oldStatus;

    @Column(name="new_status", nullable=false, length=30)
    private String newStatus;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="changed_by")
    private AppUser changedBy;

    @Column(name="note", length=500)
    private String note;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    public OrderStatusHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public AppUser getChangedBy() { return changedBy; }
    public void setChangedBy(AppUser changedBy) { this.changedBy = changedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}