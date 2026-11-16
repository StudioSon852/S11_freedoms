package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "store_settings")
public class StoreSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="setting_id")
    private Long id;

    @Column(name="setting_key", nullable=false, unique=true, length=80)
    private String settingKey;

    @Column(name="setting_value", length=500)
    private String settingValue;

    public StoreSetting() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
}