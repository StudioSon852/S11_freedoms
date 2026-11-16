package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "colors")
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="color_id")
    private Long id;

    @Column(name="color_name", nullable=false, unique=true, length=50)
    private String colorName;

    @Column(name="color_hex", length=10)
    private String colorHex;

    public Color() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}