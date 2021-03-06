/*
 * 文件名：	CityCoord.java
 * 日期：	2010-1-13
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	城市名称，城市代码，中心点（20级像素坐标）转换<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class CityCoord {
	/** 城市代码中心点对照表 */
	private static String[][] CITY_COORD = {
		{"全国", "000", "221010326", "101713397"},
	    {"北京", "010", "221010326", "101713397"},
	    {"天津", "022", "221647439", "102490724"},
	    {"上海", "021", "224796735", "109688998"},
	    {"重庆", "023", "213665993", "111132179"},
//	  河北
	    {"石家庄", "0311", "219577588", "103497563"},
	    {"保定", "0312", "220315780", "102705613"},
	    {"沧州", "0317", "221359383", "103246972"},
	    {"承德", "0314", "222156106", "100647922"},
	    {"邯郸", "0310", "219587063", "104856177"},
	    {"衡水", "0318", "220488778", "103793214"},
	    {"廊坊", "0316", "221240278", "102079179"},
	    {"秦皇岛", "0335", "223390172", "101674768"},
	    {"唐山", "0315", "222329676", "101978142"},
	    {"邢台", "0319", "219598330", "104416191"},
	    {"张家口", "0313", "219885810", "100814918"},
//	山西
	    {"太原", "0351", "218127283", "103680206"},
	    {"长治", "0355", "218563556", "105233454"},
	    {"大同", "0352", "218700164", "101547714"},
	    {"晋城", "0356", "218366826", "105878810"},
	    {"晋中", "0354", "218291861", "103837570"},
	    {"临汾", "0357", "217378205", "105316613"},
	    {"吕梁", "0358", "217082319", "103995697"},
	    {"朔州", "0349", "218051134", "102284094"},
	    {"忻州", "0350", "218278762", "103148694"},
	    {"阳泉", "0353", "218909291", "103678739"},
	    {"运城", "0359", "216983567", "106298039"},
//	内蒙古
	    {"呼和浩特", "0471", "217482417", "100832601"},
	    {"阿拉善盟", "0483", "213022921", "102737658"},
	    {"巴彦淖尔盟", "0478", "214283077", "100860339"},
	    {"包头", "0472", "216143695", "100976876"},
	    {"赤峰", "0476", "222918252", "99379038"},
	    {"鄂尔多斯", "0477", "216242144", "101796805"},
	    {"呼伦贝尔", "0470", "223514133", "91906622"},
	    {"通辽", "0475", "225382205", "98015581"},
	    {"乌海", "0473", "213864016", "101945327"},
	    {"乌兰察布", "0474", "218566929", "100612106"},
	    {"锡林郭勒盟", "0479", "220787417", "97664897"},
	    {"兴安盟", "0482", "225287957", "95405425"},
//	辽宁
	    {"沈阳", "024", "226256820", "99842502"},
	    {"鞍山", "0412", "225934921", "100528966"},
	    {"本溪", "0414", "226507686", "100355607"},
	    {"朝阳", "0421", "224032033", "100074311"},
	    {"大连", "0411", "224921999", "102665591"},
	    {"丹东", "0415", "226973050", "101500149"},
	    {"抚顺", "0413", "226622429", "99780355"},
	    {"阜新", "0418", "224929450", "99633454"},
	    {"葫芦岛", "0429", "224334651", "100891834"},
	    {"锦州", "0416", "224551992", "100518744"},
	    {"辽阳", "0419", "226066751", "100368414"},
	    {"盘锦", "0427", "225240500", "100525952"},
	    {"铁岭", "0410", "226561419", "99359512"},
	    {"营口", "0417", "225353553", "100978229"},
//	吉林
	    {"长春", "0431", "227666667", "97725590"},
	    {"白城", "0436", "225812493", "95905193"},
	    {"白山", "0439", "228486179", "99707879"},
	    {"吉林", "0432", "228596163", "97768024"},
	    {"辽源", "0437", "227532242", "98732382"},
	    {"四平", "0434", "226957179", "98464884"},
	    {"松原", "0438", "227293147", "96414192"},
	    {"通化", "0435", "228125522", "99918983"},
	    {"延边", "1433", "230778178", "98744244"},
//	黑龙江
	    {"哈尔滨", "0451", "228656443", "95754782"},
	    {"大庆", "0459", "227336431", "94833863"},
	    {"大兴安岭", "0457", "226975129", "89062091"},
	    {"鹤岗", "0468", "231360453", "94052711"},
	    {"黑河", "0456", "229289321", "90750183"},
	    {"鸡西", "0467", "231875930", "96250900"},
	    {"佳木斯", "0454", "231428333", "94625927"},
	    {"牡丹江", "0453", "230874157", "96996571"},
	    {"七台河", "0464", "231904293", "95743409"},
	    {"齐齐哈尔", "0452", "226631979", "94041123"},
	    {"双鸭山", "0469", "232017636", "94800526"},
	    {"绥化", "0455", "228908625", "94811364"},
	    {"伊春", "0458", "230333390", "93617977"},
//	江苏
	    {"南京", "025", "222801033", "108943193"},
	    {"常州", "0519", "223677502", "109212031"},
	    {"淮安", "0517", "222962080", "107579709"},
	    {"连云港", "0518", "223079431", "106693648"},
	    {"南通", "0513", "224345256", "108999633"},
	    {"苏州", "0512", "224172455", "109605082"},
	    {"宿迁", "0527", "222426108", "107272986"},
	    {"泰州", "0523", "223637193", "108602905"},
	    {"无锡", "0510", "223921062", "109377251"},
	    {"徐州", "0516", "221588960", "106998583"},
	    {"盐城", "0515", "223800017", "107787686"},
	    {"扬州", "0514", "223258414", "108640181"},
	    {"镇江", "0511", "223280458", "108824077"},
//	浙江
	    {"杭州", "0571", "223825191", "110513595"},
	    {"湖州", "0572", "223768487", "109996887"},
	    {"嘉兴", "0573", "224261822", "110098185"},
	    {"金华", "0579", "223434178", "111544883"},
	    {"丽水", "0578", "223634152", "112072329"},
	    {"宁波", "0574", "224847499", "110863107"},
	    {"绍兴", "0575", "224138379", "110746724"},
	    {"台州", "0576", "224781814", "111886772"},
	    {"温州", "0577", "224171580", "112432230"},
	    {"舟山", "0580", "225418191", "110790504"},
	    {"衢州", "0570", "222856737", "111661186"},
//	安徽
	    {"合肥", "0551", "221679782", "109124651"},
	    {"安庆", "0556", "221497186", "110294848"},
	    {"蚌埠", "0552", "221758815", "108187723"},
	    {"巢湖", "0565", "222103461", "109358382"},
	    {"池州", "0566", "221826908", "110176763"},
	    {"滁州", "0550", "222442157", "108744695"},
	    {"阜阳", "1558", "220579214", "108217155"},
	    {"淮北", "0561", "221304533", "107259395"},
	    {"淮南", "0554", "221459016", "108446667"},
	    {"黄山", "0559", "222432645", "110996913"},
	    {"六安", "0564", "221080060", "109235830"},
	    {"马鞍山", "0555", "222579248", "109279342"},
	    {"宿州", "0557", "221447127", "107556359"},
	    {"铜陵", "0562", "222063830", "109932596"},
	    {"芜湖", "0553", "222479968", "109586775"},
	    {"宣城", "0563", "222767679", "109930620"},
	    {"亳州", "0558", "220551467", "107348704"},
//	福建
	    {"福州", "0591", "223179034", "114065757"},
	    {"龙岩", "0597", "221483530", "114877212"},
	    {"南平", "0599", "222338078", "113596586"},
	    {"宁德", "0593", "223359794", "113586820"},
	    {"莆田", "0594", "222954915", "114595983"},
	    {"泉州", "0595", "222640751", "115013047"},
	    {"三明", "0598", "221936768", "113906270"},
	    {"厦门", "0592", "222280919", "115382580"},
	    {"漳州", "0596", "221955847", "115355479"},
//	江西
	    {"南昌", "0791", "220637067", "111880007"},
	    {"抚州", "0794", "220984087", "112472547"},
	    {"赣州", "0797", "219924896", "114248916"},
	    {"吉安", "0796", "219958031", "113200554"},
	    {"景德镇", "0798", "221619491", "111355561"},
	    {"九江", "0792", "220706435", "110994368"},
	    {"萍乡", "0799", "219111965", "112773101"},
	    {"上饶", "0793", "222181692", "112077922"},
	    {"新余", "0790", "219906881", "112610037"},
	    {"宜春", "0795", "219519683", "112618055"},
	    {"鹰潭", "0701", "221484385", "112252641"},
//	山东
	    {"济南", "0531", "221474610", "104800494"},
	    {"滨州", "0543", "222216798", "104124459"},
	    {"德州", "0534", "220941258", "104064007"},
	    {"东营", "0546", "222706877", "104091517"},
	    {"菏泽", "0530", "220311212", "106101081"},
	    {"济宁", "0537", "221151768", "105949565"},
	    {"莱芜", "0634", "221965184", "105213653"},
	    {"聊城", "0635", "220702813", "104989199"},
	    {"临沂", "0539", "222463218", "106277729"},
	    {"青岛", "0532", "223945658", "105359550"},
	    {"日照", "0633", "223343003", "105944534"},
	    {"泰安", "0538", "221557662", "105232047"},
	    {"威海", "0631", "225303714", "104014164"},
	    {"潍坊", "0536", "223033449", "104751235"},
	    {"烟台", "0535", "224731655", "103988537"},
	    {"枣庄", "0632", "221689432", "106496979"},
	    {"淄博", "0533", "222242240", "104663741"},
//	广东
	    {"广州", "020", "218716976", "116478929"},
	    {"潮州", "0768", "221192312", "116042452"},
	    {"东莞", "0769", "219042841", "116556475"},
	    {"佛山", "0757", "218559058", "116570769"},
	    {"河源", "0762", "219742367", "115978082"},
	    {"惠州", "0752", "219520262", "116511603"},
	    {"江门", "0750", "218246648", "117104519"},
	    {"揭阳", "0663", "220989328", "116159419"},
	    {"茂名", "0668", "216929193", "117665631"},
	    {"梅州", "0753", "220801228", "115534425"},
	    {"清远", "0763", "218510536", "116028674"},
	    {"汕头", "0754", "221253252", "116295522"},
	    {"汕尾", "0660", "220244652", "116762288"},
	    {"韶关", "0751", "218921678", "115123536"},
	    {"深圳", "0755", "219263727", "116957872"},
	    {"阳江", "0662", "217718771", "117497650"},
	    {"云浮", "0766", "217763805", "116644813"},
	    {"湛江", "0759", "216553275", "118037213"},
	    {"肇庆", "0758", "218085705", "116528155"},
	    {"中山", "0760", "218753678", "116971718"},
	    {"珠海", "0756", "218911938", "117184832"},
//	海南
	    {"海口", "0898", "216478430", "118966593"},
	    {"白沙", "0802", "215822107", "119595833"},
	    {"保亭", "0801", "216015977", "120077644"},
	    {"昌江", "0803", "215520862", "119539909"},
	    {"澄迈", "0804", "216251603", "119206138"},
	    {"定安", "0806", "216572980", "119397616"},
	    {"东方", "0807", "215265103", "119719611"},
	    {"乐东", "2802", "215624508", "119985394"},
	    {"临高", "1896", "216007774", "119073699"},
	    {"陵水", "0809", "216261297", "120163858"},
	    {"琼海", "1894", "216582674", "119587541"},
	    {"琼中", "1899", "216114403", "119746432"},
	    {"三亚", "0899", "215876998", "120384789"},
	    {"屯昌", "1892", "216312001", "119492257"},
	    {"万宁", "1898", "216528987", "119931285"},
	    {"文昌", "1893", "216797422", "119296646"},
	    {"五指山", "1897", "215879522", "119958775"},
	    {"儋州", "0805", "215925753", "119372618"},
//	广西
	    {"南宁", "0771", "214984798", "116744437"},
	    {"百色", "0776", "213720108", "115854631"},
	    {"北海", "0779", "215585716", "117813128"},
	    {"崇左", "1771", "214255827", "117052553"},
	    {"防城港", "0770", "215010030", "117697160"},
	    {"桂林", "0773", "216452085", "114722226"},
	    {"贵港", "1755", "215945964", "116511550"},
	    {"河池", "0778", "214794504", "115203876"},
	    {"贺州", "1774", "217405021", "115424653"},
	    {"来宾", "1772", "215659394", "115990540"},
	    {"柳州", "0772", "215801682", "115523828"},
	    {"钦州", "0777", "215202257", "117434230"},
	    {"梧州", "0774", "217212352", "116178938"},
	    {"玉林", "0775", "216352052", "116890541"},
//	河南
	    {"郑州", "0371", "218971033", "106543150"},
	    {"安阳", "0372", "219480358", "105308207"},
	    {"鹤壁", "0392", "219443773", "105642567"},
	    {"济源", "1391", "218169514", "106244432"},
	    {"焦作", "0391", "218670483", "106139181"},
	    {"开封", "0378", "219475973", "106518246"},
	    {"洛阳", "0379", "218029636", "106638568"},
	    {"南阳", "0377", "218129189", "108126353"},
	    {"平顶山", "0375", "218702980", "107476307"},
	    {"三门峡", "0398", "217128126", "106530531"},
	    {"商丘", "0370", "220453701", "106835700"},
	    {"新乡", "0373", "219140185", "106051701"},
	    {"信阳", "0376", "219278389", "108900753"},
	    {"许昌", "0374", "219098590", "107211643"},
	    {"周口", "0394", "219709235", "107575590"},
	    {"驻马店", "0396", "219243181", "108140681"},
	    {"漯河", "0395", "219249130", "107619718"},
	    {"濮阳", "0393", "219989773", "105628688"},
//	湖北
	    {"武汉", "027", "219430744", "110270600"},
	    {"鄂州", "0711", "219896121", "110413536"},
	    {"恩施", "0718", "215853681", "110499824"},
	    {"黄冈", "0713", "219875073", "110364538"},
	    {"黄石", "0714", "220008072", "110566780"},
	    {"荆门", "0724", "217877262", "109860261"},
	    {"荆州", "0716", "217913937", "110469340"},
	    {"潜江", "2728", "218397957", "110387560"},
	    {"神农架", "1719", "216743813", "109233927"},
	    {"十堰", "0719", "216822778", "108431589"},
	    {"随州", "0722", "218756626", "109258557"},
	    {"天门", "1728", "218598862", "110187193"},
	    {"仙桃", "0728", "218814533", "110435332"},
	    {"咸宁", "0715", "219472570", "110892881"},
	    {"襄樊", "0710", "217831838", "108955304"},
	    {"孝感", "0712", "219161541", "109949918"},
	    {"宜昌", "0717", "217205188", "110143942"},
//	湖南
	    {"长沙", "0731", "218483605", "112278825"},
	    {"常德", "0736", "217509277", "111575320"},
	    {"郴州", "0735", "218500579", "114297311"},
	    {"衡阳", "0734", "218187491", "113382488"},
	    {"怀化", "0745", "216222335", "112835663"},
	    {"娄底", "0738", "217729657", "112686271"},
	    {"邵阳", "0739", "217333751", "113097032"},
	    {"湘潭", "0732", "218410608", "112574342"},
	    {"湘西", "0743", "215941876", "112499615"},
	    {"益阳", "0737", "217996092", "111963923"},
	    {"永州", "0746", "217438622", "113764815"},
	    {"岳阳", "0730", "218557313", "111293696"},
	    {"张家界", "0744", "216595342", "111504465"},
	    {"株洲", "0733", "218588070", "112589527"},
//	四川
	    {"成都", "028", "211814834", "110181271"},
	    {"阿坝", "0837", "210440311", "109098467"},
	    {"巴中", "0827", "213819744", "109135181"},
	    {"达州", "0818", "214366799", "109699273"},
	    {"德阳", "0838", "212054821", "109765292"},
	    {"甘孜", "0836", "210174307", "110628016"},
	    {"广安", "0826", "213735612", "110343632"},
	    {"广元", "0839", "213133034", "108623415"},
	    {"乐山", "0833", "211576631", "111105011"},
	    {"凉山", "0834", "210455101", "112531425"},
	    {"眉山", "1833", "211636127", "110716354"},
	    {"绵阳", "0816", "212343082", "109486876"},
	    {"南充", "0817", "213327737", "110071761"},
	    {"内江", "1832", "212558318", "111108534"},
	    {"攀枝花", "0812", "210050090", "113652115"},
	    {"遂宁", "0825", "212946139", "110306316"},
	    {"雅安", "0835", "211031364", "110755264"},
	    {"宜宾", "0831", "212234538", "111811590"},
	    {"资阳", "0832", "212558318", "111108534"},
	    {"自贡", "0813", "212340083", "111304768"},
	    {"泸州", "0830", "212848112", "111704531"},
//	贵州
	    {"贵阳", "0851", "213785440", "113656576"},
	    {"安顺", "0853", "213202672", "113915239"},
	    {"毕节", "0857", "212726446", "113041603"},
	    {"六盘水", "0858", "212416705", "113644781"},
	    {"黔东南", "0855", "214734202", "113643977"},
	    {"黔南", "0854", "214383957", "113893818"},
	    {"黔西南", "0859", "212433109", "114878103"},
	    {"铜仁", "0856", "215633945", "112692540"},
	    {"遵义", "0852", "213951428", "112714270"},
//	云南
	    {"昆明", "0871", "210798859", "114913999"},
	    {"保山", "0875", "208160288", "114852247"},
	    {"楚雄", "0878", "209934961", "114909860"},
	    {"大理", "0872", "208948757", "114473236"},
	    {"德宏", "0692", "207714536", "115407873"},
	    {"迪庆", "0887", "208566147", "112574047"},
	    {"红河", "0873", "211154339", "116238626"},
	    {"丽江", "0888", "208989679", "113360202"},
	    {"临沧", "0883", "208846513", "115858463"},
	    {"怒江", "0886", "207965150", "114246397"},
	    {"普洱", "0879", "209516856", "116763983"},
	    {"曲靖", "0874", "211604920", "114549999"},
	    {"文山", "0876", "211955890", "116279118"},
	    {"西双版纳", "0691", "209379102", "117385955"},
	    {"玉溪", "0877", "210674056", "115490765"},
	    {"昭通", "0870", "211544857", "112989269"},
//	西藏
	    {"拉萨", "0891", "202160933", "111046112"},
	    {"阿里", "0897", "193946630", "108523296"},
	    {"昌都", "0895", "206658242", "109726274"},
	    {"林芝", "0894", "204613659", "111063340"},
	    {"那曲", "0896", "202862117", "109489911"},
	    {"日喀则", "0892", "200497708", "111374598"},
	    {"山南", "0893", "202672463", "111401137"},
//	陕西
	    {"西安", "029", "215460018", "106992862"},
	    {"安康", "0915", "215517195", "108399370"},
	    {"宝鸡", "0917", "214106269", "106898168"},
	    {"汉中", "0916", "214026085", "108054661"},
	    {"商洛", "0914", "216197251", "107337156"},
	    {"铜川", "0919", "215454948", "106417255"},
	    {"渭南", "0913", "215874507", "106773820"},
	    {"咸阳", "0910", "215277690", "106937018"},
	    {"延安", "0911", "215864236", "104861844"},
	    {"榆林", "0912", "216056017", "103263175"},
//	甘肃
	    {"兰州", "0931", "211642827", "105355557"},
	    {"白银", "0943", "211878090", "104901478"},
	    {"定西", "0932", "212229459", "105792087"},
	    {"甘南", "0941", "210944784", "106339990"},
	    {"嘉峪关", "1937", "207480922", "101818367"},
	    {"金昌", "0935", "210404992", "103091008"},
	    {"酒泉", "0937", "207674999", "101860868"},
	    {"临夏", "0930", "211170033", "105786414"},
	    {"陇南", "2935", "212461934", "107707131"},
	    {"平凉", "0933", "213797834", "105861241"},
	    {"庆阳", "0934", "214645093", "105471197"},
	    {"天水", "0938", "213034571", "106701284"},
	    {"武威", "1935", "210746854", "103607871"},
	    {"张掖", "0936", "209119324", "102656649"},
//	青海
	    {"西宁", "0971", "210107883", "104839632"},
	    {"果洛", "0975", "209003100", "106800815"},
	    {"海北", "0970", "209508588", "104536058"},
	    {"海东", "0972", "210338761", "104939940"},
	    {"海南", "0974", "209258655", "105111176"},
	    {"海西", "0977", "206816992", "104088610"},
	    {"黄南", "0973", "210294573", "105833610"},
	    {"玉树", "0976", "206555864", "108109446"},
//	宁夏
	    {"银川", "0951", "213468234", "103103943"},
	    {"固原", "0954", "213466585", "105400631"},
	    {"石嘴山", "0952", "213545106", "102576463"},
	    {"吴忠", "0953", "213402062", "103557292"},
	    {"中卫", "1953", "213022922", "104030850"},
//	新疆
	    {"乌鲁木齐", "0991", "199549549", "97791140"},
	    {"阿克苏", "0997", "194052364", "100520915"},
	    {"阿勒泰", "0906", "199886734", "93523376"},
	    {"巴音郭楞", "0996", "198480207", "99750681"},
	    {"博尔塔拉", "0909", "195420054", "96655104"},
	    {"昌吉", "0994", "199826507", "97603835"},
	    {"哈密", "0902", "203973813", "98739207"},
	    {"和田", "0903", "193694152", "104268460"},
	    {"喀什", "0998", "190886988", "102142130"},
	    {"克拉玛依", "0990", "197546985", "95930781"},
	    {"克孜勒苏", "0908", "191387024", "101733198"},
	    {"石河子", "0993", "198383227", "97291398"},
	    {"塔城", "0901", "196118040", "94686025"},
	    {"吐鲁番", "0995", "200738041", "98629321"},
	    {"伊犁", "0999", "194867982", "97687932"},
	};
	
	/**
	 * 返回城市名称对应的该城市的信息字符串数组表示形式
	 * @param cityName
	 * @return 城市名称对应的该城市的信息字符串数组表示形式，String[0]=城市名称，String[1]=城市代码，String[2]=20级像素x坐标，String[3]=20级像素y坐标
	 */
	public static String[] getCityCoord(String cityName) {
		int len = CITY_COORD.length;
		for (int i = 0; i < len; i++) {
			if(CITY_COORD[i][0].equals(cityName) ) {
				return 	CITY_COORD[i];
			}
		}
		return null;
	}
	
	/**
	 * 返回城市名称对应的城市代码
	 * @param cityName
	 * @return 城市名称对应的城市代码
	 */
	public static String getCityCode(String cityName) {
		int len = CITY_COORD.length;
		for (int i = 0; i < len; i++) {
			if(CITY_COORD[i][0].equals(cityName) ) {
				return 	CITY_COORD[i][1];
			}
		}
		return null;
	}
	
	/**
	 * 返回城市代码对应的城市名称
	 * @param code
	 * @return 城市代码对应的城市名称
	 */
	public static String getCity(String code) {
		int len = CITY_COORD.length;
		for (int i = 0; i < len; i++) {
			if(CITY_COORD[i][1].equals(code) ) {
				return 	CITY_COORD[i][0];
			}
		}
		return null;
	}
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		System.out.println(CityCoord.getCityCode("北京"));
//
//	}

}
