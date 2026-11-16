package vn.shoegroup.sd11.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class RegisterForm {
    @NotBlank(message="Vui lòng nhập họ tên") @Size(max=150) private String fullName;
    @NotBlank(message="Vui lòng nhập email") @Email(message="Email không đúng định dạng") @Size(max=190) private String email;
    @NotBlank(message="Vui lòng nhập mật khẩu") @Size(min=8,max=72,message="Mật khẩu phải có ít nhất 8 ký tự") private String password;
    @NotBlank(message="Vui lòng nhập lại mật khẩu") private String confirmPassword;
    private String phone;
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getConfirmPassword(){return confirmPassword;} public void setConfirmPassword(String v){confirmPassword=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
}
