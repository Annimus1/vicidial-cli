package dev.pablo.models;

/**
 * Representa un unico objeto Lead
 */
public class LeadModel {
    private String status;
    private String user;
    private String vendor_lead_code;
    private String source_id;
    private String list_id;
    private String gmt_offset_now;
    private String phone_code;
    private String phone_number;
    private String title;
    private String first_name;
    private String middle_initial;
    private String last_name;
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String state;
    private String province;
    private String postal_code;
    private String country_code;
    private String gender;
    private String date_of_birth;
    private String alt_phone;
    private String email;
    private String security_phrase;
    private String comments;
    private String called_count;
    private String last_local_call_time;
    private String rank;
    private String owner;
    private String entry_list_id;
    private String lead_id;

    /**
     *  Crea un Objeto Lead a partir
     *  de la respuesta de la api Vicidial.
     *  Divide los campos basandose en el delimitador '|'.
     */
    public LeadModel(String rowInput) {
        String[] details = rowInput.split("\\|");
        for (int i = 0; i < details.length; i++) {
            switch (i) {
                case 0:
                    this.status = details[i];
                    break;
                case 1:
                    this.user = details[i];
                    break;
                case 2:
                    this.vendor_lead_code = details[i];
                    break;
                case 3:
                    this.source_id = details[i];
                    break;
                case 4:
                    this.list_id = details[i];
                    break;
                case 5:
                    this.gmt_offset_now = details[i];
                    break;
                case 6:
                    this.phone_code = details[i];
                    break;
                case 7:
                    this.phone_number = details[i];
                    break;
                case 8:
                    this.title = details[i];
                    break;
                case 9:
                    this.first_name = details[i];
                    break;
                case 10:
                    this.middle_initial = details[i];
                    break;
                case 11:
                    this.last_name = details[i];
                    break;
                case 12:
                    this.address1 = details[i];
                    break;
                case 13:
                    this.address2 = details[i];
                    break;
                case 14:
                    this.address3 = details[i];
                    break;
                case 15:
                    this.city = details[i];
                    break;
                case 16:
                    this.state = details[i];
                    break;
                case 17:
                    this.province = details[i];
                    break;
                case 18:
                    this.postal_code = details[i];
                    break;
                case 19:
                    this.country_code = details[i];
                    break;
                case 20:
                    this.gender = details[i];
                    break;
                case 21:
                    this.date_of_birth = details[i];
                    break;
                case 22:
                    this.alt_phone = details[i];
                    break;
                case 23:
                    this.email = details[i];
                    break;
                case 24:
                    this.security_phrase = details[i];
                    break;
                case 25:
                    this.comments = details[i];
                    break;
                case 26:
                    this.called_count = details[i];
                    break;
                case 27:
                    this.last_local_call_time = details[i];
                    break;
                case 28:
                    this.rank = details[i];
                    break;
                case 29:
                    this.owner = details[i];
                    break;
                case 30:
                    this.entry_list_id = details[i];
                    break;
                case 31:
                    this.lead_id = details[i];
                    break;
                default:
                    // ignore extra fields
                    break;
            }
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getVendor_lead_code() {
        return vendor_lead_code;
    }

    public void setVendor_lead_code(String vendor_lead_code) {
        this.vendor_lead_code = vendor_lead_code;
    }

    public String getSource_id() {
        return source_id;
    }

    public void setSource_id(String source_id) {
        this.source_id = source_id;
    }

    public String getList_id() {
        return list_id;
    }

    public void setList_id(String list_id) {
        this.list_id = list_id;
    }

    public String getGmt_offset_now() {
        return gmt_offset_now;
    }

    public void setGmt_offset_now(String gmt_offset_now) {
        this.gmt_offset_now = gmt_offset_now;
    }

    public String getPhone_code() {
        return phone_code;
    }

    public void setPhone_code(String phone_code) {
        this.phone_code = phone_code;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getMiddle_initial() {
        return middle_initial;
    }

    public void setMiddle_initial(String middle_initial) {
        this.middle_initial = middle_initial;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostal_code() {
        return postal_code;
    }

    public void setPostal_code(String postal_code) {
        this.postal_code = postal_code;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDate_of_birth() {
        return date_of_birth;
    }

    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    public String getAlt_phone() {
        return alt_phone;
    }

    public void setAlt_phone(String alt_phone) {
        this.alt_phone = alt_phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSecurity_phrase() {
        return security_phrase;
    }

    public void setSecurity_phrase(String security_phrase) {
        this.security_phrase = security_phrase;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCalled_count() {
        return called_count;
    }

    public void setCalled_count(String called_count) {
        this.called_count = called_count;
    }

    public String getLast_local_call_time() {
        return last_local_call_time;
    }

    public void setLast_local_call_time(String last_local_call_time) {
        this.last_local_call_time = last_local_call_time;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getEntry_list_id() {
        return entry_list_id;
    }

    public void setEntry_list_id(String entry_list_id) {
        this.entry_list_id = entry_list_id;
    }

    public String getLead_id() {
        return lead_id;
    }

    public void setLead_id(String lead_id) {
        this.lead_id = lead_id;
    }

    @Override
    public String toString() {
        return "status: " + status +
                "\nuser: " + user +
                "\nvendor_lead_code: " + vendor_lead_code +
                "\nsource_id: " + source_id +
                "\nlist_id: " + list_id +
                "\ngmt_offset_now: " + gmt_offset_now +
                "\nphone_code: " + phone_code +
                "\nphone_number: " + phone_number +
                "\ntitle: " + title +
                "\nfirst_name: " + first_name +
                "\nmiddle_initial: " + middle_initial +
                "\nlast_name: " + last_name +
                "\naddress1: " + address1 +
                "\naddress2: " + address2 +
                "\naddress3: " + address3 +
                "\ncity: " + city +
                "\nstate: " + state +
                "\nprovince: " + province +
                "\npostal_code: " + postal_code +
                "\ncountry_code: " + country_code +
                "\ngender: " + gender +
                "\ndate_of_birth: " + date_of_birth +
                "\nalt_phone: " + alt_phone +
                "\nemail: " + email +
                "\nsecurity_phrase: " + security_phrase +
                "\ncomments: " + comments +
                "\ncalled_count: " + called_count +
                "\nlast_local_call_time: " + last_local_call_time +
                "\nrank: " + rank +
                "\nowner: " + owner +
                "\nentry_list_id: " + entry_list_id +
                "\nlead_id: " + lead_id;
    }
}
