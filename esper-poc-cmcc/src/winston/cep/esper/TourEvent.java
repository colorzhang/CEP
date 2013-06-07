package winston.cep.esper;

public class TourEvent {

	private String imsi;
	private String signalingTime;
	private String lac;
	private String cell;
	private String mytime;

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public String getSignalingTime() {
		return signalingTime;
	}

	public void setSignalingTime(String signalingTime) {
		this.signalingTime = signalingTime;
	}

	public String getLac() {
		return lac;
	}

	public void setLac(String lac) {
		this.lac = lac;
	}

	public String getCell() {
		return cell;
	}

	public void setCell(String cell) {
		this.cell = cell;
	}

	public String getMytime() {
		return mytime;
	}

	public void setMytime(String mytime) {
		this.mytime = mytime;
	}

}
