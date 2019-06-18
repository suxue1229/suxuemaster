package engine;

import java.util.Dictionary;
import java.util.Hashtable;

public class MBrane {
	// region basic features
	// 膜直径 inch
	public double width;
	// 膜长度 inch
	public double length;
	// 膜面积 m2
	public double area;

	// 水力透过系数
	public double parA;
	//
	public double parw;
	//
	private double park;
	//
	private double park0;
	//
	private double parx0u;
	// 膜元件面积 m2
	public double parSE;
	// 浓水流道截面积 m2
	private double parAE;
	// endregion

	// region streams
	// 进水
	public MStream streamf;
	// 浓水
	public MStream streamc;
	// 产水
	public MStream streamp;
	// 产水背压 MPa
	public double parPpi;
	// endregion

	Dictionary<EIon, MBIon> ipass = new Hashtable<>();

	// region constant calculation
	// 温度修正因子
	private double parTCF() {
		return Math.exp(parw * (streamf.parT() - 25));
	}

	// 进水SO4离子强度占比
	private double parXuSO4() {
		double xu = 0;
		for (EIon ion : EIon.values()) {
			xu += Math.pow(streamf.ion(ion).parZj, 2) * streamf.ion(ion).parmj();
		}
		xu = 4 * streamf.ion(EIon.SO4).parmj() / xu;
		return xu;
	}

	// 浓水压降 MPa
	private double parDPfc() {
		return (park * Math.pow(streamf.parQ - parJp * parSE / 1000 / 2, 2) + park0) / 1000;
	}

	// 平均渗透压
	private double parpifc() {
		double fc = 0;
		for (EIon ion : EIon.values()) {
			fc += parBj(ion) * (1 + (1 - parYt() * (1 - parRjd(ion))) / (1 - parYt())) * streamf.ion(ion).parmj();
		}
		fc = 8.314 / 1000 * (273.15 + streamf.parT()) / 2 * fc;
		return fc;
	}

	// 产水渗透压
	private double parpip() {
		double p = 0;
		for (EIon ion : EIon.values()) {
			p += parBj(ion) * parTrjd(ion) * streamf.ion(ion).parmj();
		}
		p = 8.314 / 1000 * (273.15 + streamf.parT()) * p;
		return p;
	}

	// 水通量
	private boolean firstJp = true;
	private double parJp = 0;

	private double parJp() {
		if (firstJp) {
			firstJp = false;
			parJp = parA * parTCF() * (streamf.parP - parPpi);
		} else {
			parJp = parA * parTCF() * (streamf.parP - parDPfc() / 2 - parPpi - (parpifc() - parpip()));
		}
		return parJp;
	}

	// 回收率
	public double parYt() {
		return parJp * parSE / 1000 / streamf.parQ;
	}

	// 离子透过率
	private double parTrj(EIon ion) {
		double temp = 1.0
				/ (2 * (1 - ipass.get(ion).Pj) / (ipass.get(ion).Pj + 2 * ipass.get(ion).Ds / (int) parJp) + 1);
		if (Double.isInfinite(temp) || Double.isNaN(temp)) {
			throw new ArithmeticException("离子透过率计算错误");
		} else {
			return 1.0 / (2 * (1 - ipass.get(ion).Pj) / (ipass.get(ion).Pj + 2 * ipass.get(ion).Ds / (int) parJp) + 1);
		}
	}

	// 离子截留率
	private double parRj(EIon ion) {
		return 1 - parTrj(ion);
	}

	// 修正离子透过率
	private double parRjd(EIon ion) {
		return parRj(ion) + ipass.get(ion).qj * (parXuSO4() - parx0u);
	}

	private double parTrjd(EIon ion) {
		return 1 - parRjd(ion);
	}

	// 膜面流速
	private double parut() {
		return streamf.parQ * 1000 * (1 - parYt() / 2) / parAE;
	}

	// 浓差极化系数
	// b_j=k_(j,b)×(273.15+T)+b_(j,b)
	private double bj(EIon ion) {
		return ipass.get(ion).kb * (273.15 + streamf.parT()) + ipass.get(ion).bb;
	}

	private double parBj(EIon ion) {
		return Math.exp(bj(ion) * parJp / parut())
				/ (parRjd(ion) + (1 - parRjd(ion)) * Math.exp(bj(ion) * parJp / parut()));
	}

	// // COD透过系数
	// private double parRCOD(double Mj) {
	// if (Mj == 0) {
	// return damCOD;
	// }
	// if (Mj <= 42) {
	// return damMin;
	// }
	// if (Mj > 400) {
	// return damMax;
	// }
	// return 1 - damCoe * Math.exp(-Mj / damDiv);
	// }

	// 通量 LMH
	public double parFi() {
		return this.streamp.parQ * 1000 / this.parSE;
	}
	// endregion

	public MBrane(String name) throws Exception {
		if (name.equals("DF90-8040(400)")) {
			this.width = 8;
			this.length = 40;
			this.area = 37.6;
			this.parA = 78.68;
			this.parw = 0.0413;
			this.park = 0.3673;
			this.park0 = 4.2288;
			this.parx0u = 0.1418;
			this.parSE = 37.6;
			this.parAE = 0.0180;
			ipass.put(EIon.K, new MBIon(0.0137, 1.5809, 0, 1962.6, 0.2874));
			ipass.put(EIon.Na, new MBIon(0.0424, 1.4134, 0, 2349.5, -0.0863));
			ipass.put(EIon.NH4, new MBIon(0.1027, 1.2575, 0, 1529.9, 0.1994));
			ipass.put(EIon.Ca, new MBIon(0.0004, 0.1164, 0, 2187.8, 0.0422));
			ipass.put(EIon.Mg, new MBIon(0.0004, 0.2142, 0, 3279.5, 0.0403));
			ipass.put(EIon.Ba, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Sr, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Fe2, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Mn, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Fe3, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Al, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.NO3, new MBIon(0.1538, 4.8784, 0, 1756.3, 0.0413));
			ipass.put(EIon.F, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Cl, new MBIon(0.0271, 0.6947, 0, 2452.9, 0.0227));
			ipass.put(EIon.HCO3, new MBIon(0.0161, 0.0551, 0, 0, 0.0054));
			ipass.put(EIon.SO4, new MBIon(0.0029, 0.0112, 0, 6244.1, 0));
			ipass.put(EIon.P, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.PO4, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.HPO4, new MBIon(0.0024, 0.0004, 0, 4771.2, -0.0024));
			ipass.put(EIon.H2PO4, new MBIon(0.0024, 0.0004, 0, 4771.2, -0.0024));
		} else if (name.equals("DF30-8040(400)")) {
			this.width = 8;
			this.length = 40;
			this.area = 37;
			this.parA = 92;
			this.parw = 0.03488;
			this.park = 0.3802;
			this.park0 = 4.0815;
			this.parx0u = 0.1245;
			this.parSE = 37;
			this.parAE = 0.0159;
			ipass.put(EIon.K, new MBIon(0.6244, 25.7754, 0, 0, 1.1609));
			ipass.put(EIon.Na, new MBIon(0.5412, 34.1303, 0, 0, 1.1609));
			ipass.put(EIon.NH4, new MBIon(0.6247, 7.8787, 0, 0, 1.1609));
			ipass.put(EIon.Ca, new MBIon(0.6519, 2.7940, 0, 0, 1.4391));
			ipass.put(EIon.Mg, new MBIon(0.6321, 2.4276, 0, 0, 1.4391));
			ipass.put(EIon.Ba, new MBIon(0.6520, 2.7940, 0, 0, 1.4391));
			ipass.put(EIon.Sr, new MBIon(0.6519, 2.7940, 0, 0, 1.4391));
			ipass.put(EIon.Fe2, new MBIon(0.66, 2.4, 0, 0, 0));
			ipass.put(EIon.Mn, new MBIon(0.65, 2.02, 0, 0, 0));
			ipass.put(EIon.Fe3, new MBIon(0.03, 0.6, 0, 0, 0));
			ipass.put(EIon.Al, new MBIon(0.04, 0.65, 0, 0, 0));
			ipass.put(EIon.NO3, new MBIon(1.0507, 1.5927, 0, 0, -1.0789));
			ipass.put(EIon.F, new MBIon(0.3965, 23.8206, 0, 0, 0));
			ipass.put(EIon.Cl, new MBIon(1.0039, 9.7, 0, 0, -0.5));
			ipass.put(EIon.HCO3, new MBIon(0.5273, 20.3184, 0, 0, 0.065));
			ipass.put(EIon.SO4, new MBIon(0.0415, 0.6144, 0, 0, 0));
			ipass.put(EIon.P, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.PO4, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.HPO4, new MBIon(0.0366, 0.5227, 0, 0, -0.1107));
			ipass.put(EIon.H2PO4, new MBIon(0.0366, 0.5227, 0, 0, -0.1107));
		} else if (name.equals("DF304I-8040(400)")) {
			this.width = 8;
			this.length = 40;
			this.area = 37.6;
			this.parA = 111.16;
			this.parw = 0.03280;
			this.park = 0.3529;
			this.park0 = 3.3757;
			this.parx0u = 0.1339;
			this.parSE = 37.6;
			this.parAE = 0.0172;
			ipass.put(EIon.K, new MBIon(0.7510, 6.1899, -28.5, 9788.5, 1.1646));
			ipass.put(EIon.Na, new MBIon(0.7355, 7.0374, -139.0, 43674.6, 1.2181));
			ipass.put(EIon.NH4, new MBIon(0.7173, 2.5167, 0, 0, 0.7647));
			ipass.put(EIon.Ca, new MBIon(0.4607, 5.0781, 0, 1891.3, 1.3815));
			ipass.put(EIon.Mg, new MBIon(0.2911, 5.2535, 0, 667.1, 0.9228));
			ipass.put(EIon.Ba, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Sr, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Fe2, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Mn, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Fe3, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Al, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.NO3, new MBIon(1.0229, 4.5631, 0, 0, -1.4001));
			ipass.put(EIon.F, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.Cl, new MBIon(0.9991, 1.4891, 0, 3765.9, -1.1274));
			ipass.put(EIon.HCO3, new MBIon(0.2018, 13.9941, -130.5, 40784.2, -0.2625));
			ipass.put(EIon.SO4, new MBIon(0.0045, 0.0039, 0, 2897.2, 0));
			ipass.put(EIon.P, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.PO4, new MBIon(0.5, 0.5, 0, 0, 0));
			ipass.put(EIon.HPO4, new MBIon(0.0060, 0.0001, 0, 6676.6, 0));
			ipass.put(EIon.H2PO4, new MBIon(0.0948, 0.4796, 0, 812.5, 0));
		} else {
			throw new Exception("unsupported membrane model");
		}
	}

	public void calculate(double mfH2CO3) throws Exception {
		int count = 0;
		firstJp = true;
		double Jp = parJp();
		while (true) {
			double Jpn = 0;
			if (Jp > 0) {
				Jpn = parJp();
				MLogger.memlog(String.format("Jp %f -> %f, %f%%", Jp, Jpn, 100 * Math.abs((Jpn - Jp) / Jp)));
				if (Jpn == Double.POSITIVE_INFINITY || Jpn == Double.NaN) {
					throw new ArithmeticException("元件水通量计算错误");
				}
				if (Jpn > 0) {
					Jp = Jpn;
					count++;
				} else {
					Jp = 0;
				}
			} else {
				Jp = 0;
				break;
			}
			if (count == 20) {
				break;
			}
		}
		parJp = Jp;
		streamp = streamf.copy();
		streamp.parQ = parJp * parSE / 1000;
		streamc = streamf.copy();
		streamc.parQ = streamf.parQ - streamp.parQ;
		for (EIon ion : EIon.values()) {
			streamp.ion(ion).parcj(parBj(ion) * parTrjd(ion) * streamf.ion(ion).parcj());
			streamc.ion(ion)
					.parcj((1 - parYt() * parBj(ion) * parTrjd(ion)) / (1 - parYt()) * streamf.ion(ion).parcj());
		}
		for (MCOD c : streamf.cods()) {
			streamp.cod(c.name).parcj = (1 - c.parRCOD) * c.parcj;
			streamc.cod(c.name).parcj = (1 - parYt() * (1 - c.parRCOD)) / (1 - parYt()) * c.parcj;
		}
		streamp.parP = parPpi;
		streamc.parP = streamf.parP - parDPfc();
		streamc.updpH(mfH2CO3);
		this.streamp.ion(EIon.PO4).ioncp_p = this.streamp.ion(EIon.PO4).parcj() * this.streamp.parQ;
		this.streamp.ion(EIon.HPO4).ioncp_p = this.streamp.ion(EIon.HPO4).parcj() * this.streamp.parQ;
		this.streamp.ion(EIon.H2PO4).ioncp_p = this.streamp.ion(EIon.H2PO4).parcj() * this.streamp.parQ;
	}
}
