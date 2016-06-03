package main;
/*
 * File: S3Reader.java
 * Author(s): Ethan Allnutt & Josh Testa
 * Company: Praxis Engineering (praxiseng.com)
 * Date Last Modified: 5/31/16
 * Project: Interns Summer 2016
 * 
 * This is the main class that downloads the file(s) from the S3 bucket from AWS
 * Then parses file(s) by using two filters,
 * It discards everything that is deemed a 'Common Word' that is in commonWords.txt
 * It then counts the occurrences of any 'Buzz Words' that is in buzzWords.txt
 * Everything else is deemed a 'New Word' and printed under a 'NEW WORD' label with the number of occurrences
 * 
 *  To include/exclude more/less words one just has to modify one or both file mentioned above
 *  Any words that are in both files are caught by the first filter and not reported
 *  
 *  As of now compound words (i.e. command line, mechanical engineer) are parsed together 
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * This class reads a specified object/file from S3 and writes locally
 */
public class S3Reader {

	public class Word {

		private String name;
		private int count;

		public Word(String name) {
			this.name = name;
			count = 1;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public void addOne() {
			count++;
		}

		public boolean equals(Word obj) {
			return this.name.equalsIgnoreCase(obj.name);
		}

		public boolean equals(String obj) {
			return this.name.equalsIgnoreCase(obj);
		}

		public String toString() {
			return name;
		}

		public int compareTo(Word obj) {
			return name.compareTo(obj.getName());
		}

	}

	//Used to store the input after common words are removed
	String totalText = "";	

	//Used to login to AWS
	AmazonS3 s3;

	//Stores common words from commonWords.txt
	ArrayList<String> excludedWords = new ArrayList<String>(Arrays.asList("&accreditation","+","02","0600","1+","100lbs",
			"10g","11","11g","12","12+","12c","13",
			"14","15","15+","16+","1800","2+","20",
			"2005","2007","2008","2012","2013","25","25+",
			"30","360p","3d","5+","50","50%","50+",
			"500","508","509","6+","8am","I","a",
			"a&a","a&se","a11y","abilities","ability","able","about",
			"above","abreast","abroad","absorbed","abstracts","academic","academics",
			"accept","acceptable","acceptance","accepted","accepting","access","accesses",
			"accessibility","accessible","accessing","accessories","accommodate","accommodations","accomplish",
			"accomplishing","accomplishment","accomplishments","accordance","according","accordingly","account",
			"accounted","accounting","accounts","accreditation","accreditations","accredited","accrediting",
			"accumulo","accurately","acfs","acg","achievability","achievable","achieve",
			"achievement","achievements","achieving","aci","acknowledge","acquire","acquired",
			"acquiring","acquisition","acquisitions","across","act","acting","action",
			"actionable","actions","actionscript","active","actively","activemq","activities",
			"activity","actual","acumen","acute","ad","adapt","add",
			"added","adding","addition","additional","additionally","additions","address",
			"addressed","addresses","addressing","adds","adequate","adhere","adhered",
			"adherence","adheres","adhering","adhoc","adjudicating","adjust","adjusting",
			"adjustments","admin","administer","administered","administering","administers","administrating",
			"administration","administrations","administrative","administrator","administrators","adobe","adopt",
			"adoptable","adopted","adoption","advance","advanced","advancements","advances",
			"advantage","advantageous","advent","adverb","adversarial","adversary","advertising",
			"advice","advise","adviser","advising","advisor","advisors","advisory",
			"advocate","advocates","aerb","affect","affected","affecting","affiliated",
			"affiliates","after","afterwards","again","against","agencies","agency",
			"agendas","agent","agents","aggregate","aggregating","aggregation","aggregators",
			"aggressive","agility","agnostic","agree","agreed","agreement","agreements",
			"agular","ahead","aid","aided","aids","aim","aimed",
			"aircraft","aka","akin","alerting","alerts","algorithm","algorithms",
			"align","aligned","aligning","alignment","alignments","aligns","alike",
			"all","allocated","allocating","allocation","allocations","allow","allowed",
			"allowing","allows","almost","along","alongside","aloud","already",
			"also","alterations","alternate","alternative","alternatives","although","always",
			"am","amazing","amazon","ambiguity","ambiguous","amendments","among",
			"amounts","an","analyses","analysis","analysisthe","analyst","analysts",
			"analytic","analytically","analyze","analyzers","analyzes","analyzing","and",
			"angles","angular","animate","animation","animations","annotations","announce",
			"annual","anomalies","anomalous","another","ansible","answer","answering",
			"anti","anticipate","anticipated","anticipation","antivirus","any","anyone",
			"anything","ap","api","api's","apic","apis","app",
			"appdetective","apple","applets","appliances","applicability","applicable","applicant",
			"applicant's","application's","applied","applies","apply","applying","apppurify",
			"appreciation","apprised","approach","approached","approaches","appropriate","appropriately",
			"approval","approvals","approved","approvers","approx","apps","aptana",
			"aptitude","architect","architecting","architects","architectural","architecture","architectures",
			"archival","archive","archiving","are","area","areas","argued",
			"arise","around","array","arrays","art","artforms","articles",
			"articulate","articulated","articulating","artifact","artificatory","artwork","as",
			"asa","asas","ask","asked","asking","asm","asp",
			"aspect","aspects","aspirational","assemble","assembling","assembly","assess",
			"assessed","assessing","asset","assign","assigned","assigning","assignment",
			"assignments","assigns","assimilate","assist","assistance","assisted","assisting",
			"assistive","assists","associate","associated","associates","assume","assurance",
			"assure","assuring","at","atdd","ato","attached","attack",
			"attacks","attain","attend","attendance","attendees","attending","attends",
			"attention","attitude","attributable","attribute","attributes","audience","audiences",
			"audio","audit","auditable","auditd","audits","augment","augmented",
			"auth","author","authored","authoring","authoritative","authorities","authority",
			"authorization","authorized","authors","automated","automatic","automatically","automating",
			"autonomy","av","availability","available","avenues","avoid","avro",
			"award","aware","awareness","awscli","azure","b","b&f",
			"bachelor","bachelor's","back","backbone","backed","backend","background",
			"backgrounds","backing","backlog","backlogs","backups","bag","balance",
			"balanced","balancers","balances","balancing","bamboo","banking","bar",
			"bare","barriers","base","based","baseline","baselines","baselining",
			"bases","basic","basics","basis","batch","battery","bayesian",
			"bcp","bdd","be","beautifulwe","because","become","been",
			"before","begin","beginning","behalf","behavior","behavioral","behaviors",
			"behind","being","below","benchmark","benefit","benefits","best",
			"better","between","beyond","bi","big","billing","bing",
			"biographic","biometrics","blackboard","blade","blades","blended","blender",
			"blending","blis","block","blocks","blog","blogs","bloom's",
			"blue","bluetooth","blur","bmd","bmp","board","boarding",
			"boards","bodies","body","boe","bookmarking","boolean","both",
			"boundaries","boundary","box","boxes","bpm","brainpower","brainstorming",
			"branch","branching","brand","branded","branding","breadth","breakdown",
			"breaks","breakthroughs","breed","bridge","bridging","brief","briefing",
			"briefings","briefs","bring","bringing","broad","broaden","broader",
			"broadly","brocade","brochures","broken","broker","brokered","brown",
			"browser","browsers","bs","bsd","budget","budgetary","budgets",
			"bug","bugs","build","builder","building","buildings","builds",
			"buildtools","built","bulk","bulletin","burden","bureaucratic","burn",
			"burpsuite","bus","business","busy","but","buttons","by",
			"bytecode","c&a","c++0x","c99","cable","cables","cabling",
			"caching","cad","cadence","cadre","caffe","calculate","calculations",
			"calendars","caliber","call","called","calls","camel","can",
			"candidacy","candidate","candidate's","candidates","cannot","capabilities","capability",
			"capable","capacity","capistrano","capital","capitalizes","captioning","capture",
			"captured","captures","capturing","cards","career","carrier","carries",
			"carry","carrying","cascading","case","cases","cassandra","catalog",
			"cataloging","catalogs","categories","categorization","categorize","categorizing","category",
			"cause","causes","cb","cbt","cc","ccie","ccna",
			"ccnp","cdh","cds","cell","cellular","cent","center",
			"centered","centers","centos","central","centralized","centric","centrify",
			"certain","certificate","certification","certifications","certified","chain","chaining",
			"chains","chair","challenge","challenged","challenges","challenging","champions",
			"chance","change","changer","changes","changing","channel","channels",
			"character","characteristics","characterization","characterize","charged","charging","chartered",
			"charters","charts","check","checklist","checks","chief","chiefs",
			"choices","chosen","chrome","ci","cic","cifs","circuit",
			"cis","cisco","cissp","citing","clamav","clarification","clarify",
			"class","classes","classification","classifications","classified","classify","classroom",
			"classrooms","clean","cleaning","cleanse","cleansing","clear","cleared",
			"clearly","cli","client","client's","clients","close","closed",
			"closely","closing","closure","closures","cloud","cloudera","cloudformation",
			"clouds","clr","cluster","clustered","clustering","clusters","cm",
			"cms","co","coaching","code","coded","codes","codify",
			"coding","cognitive","cognitively","cognizance","cognizant","coherent","cohesive",
			"cohesively","cohort","coldfusion","collaborate","collaborates","collaborating","collaboration",
			"collaborations","collaborative","collaboratively","collate","collateral","colleagues","collect",
			"collected","collecting","collection","collections","collective","collectively","collectors",
			"college","column","combination","combine","combined","combining","come",
			"comes","comfort","comfortable","comm","comma","commanding","commands",
			"commencing","commensurate","comments","commercial","commercially","commitment","commitments",
			"committed","committee","common","commonly","communicate","communicated","communicates",
			"communicating","communication","communications","communities","community","compact","companies",
			"companion","company","comparable","compare","comparing","compatibility","compatible",
			"compelling","competencies","competency","competing","competitive","compile","compiling",
			"complementary","complete","completed","completely","completeness","completing","completion",
			"complex","complexity","compliance","compliant","complies","comply","component",
			"components","compose","composed","composes","composing","composition","compound",
			"comprehending","comprehension","comprehensive","comprehensively","compression","comprised","comprises",
			"computational","compute","computer","computerized","computers","computing","con",
			"conceive","conceiving","concentrate","concentrated","concept","conception","concepts",
			"conceptual","conceptualization","conceptualize","conceptualized","conceptualizing","conceptually","concerned",
			"concerning","concerns","concert","concise","concisely","conclusion","conclusions",
			"concrete","concurrent","concurrently","condition","conditioning","condor","conduct",
			"conducted","conducting","conducts","conduit","confer","conference","conferences",
			"confidence","confident","confidentiality","confidently","configurate","configuration","configurations",
			"configure","configured","configuring","confirming","conflict","conflicting","confliction",
			"conflicts","conform","conforming","conformity","conjunction","connect","connecting",
			"connection","connections","connectivity","conops","conscious","consecutive","consensus",
			"consequences","consider","considerable","consideration","considerations","considered","considering",
			"consist","consistency","consistent","consistently","consisting","consists","consolidate",
			"consolidated","consolidation","constant","constantly","constraints","construct","constructing",
			"construction","constructive","constructs","consult","consultants","consultation","consultations",
			"consultative","consulting","consults","consumable","consumed","consumer","consumers",
			"consuming","consumption","contact","contacts","contained","container","containing",
			"contemporary","content","contents","context","contexts","contiguous","continent",
			"continental","contingencies","contingency","continual","continually","continue","continued",
			"continuity","continuously","contract","contractor","contractors","contracts","contribute",
			"contributes","contributing","control","controlled","controller","controllers","controlling",
			"controls","converging","conversations","conversion","conversions","convert","converting",
			"convey","conveyance","conveying","cooling","coop","cooperatively","coordinate",
			"coordinated","coordinates","coordinating","coordination","coordinator","copper","cops",
			"copy","copyright","core","coreldraw","cornerstone","corporate","correct",
			"correction","corrections","corrective","correctly","correctness","correlate","correspondence",
			"corresponding","corrupted","cors","cost","costs","could","counsel",
			"countermeasures","counterpart","counterparts","counters","counts","course","courses",
			"cover","coverage","covering","cpm","cradle","craft","crafting",
			"create","created","creates","creating","creation","creative","creativity",
			"credentialed","credible","criteria","critical","critique","cron","cross",
			"crowd","crowdfunding","crowdsourced","crowdsourcing","crs","cruise","crunch",
			"cryptographic","cs","cs6","csfc","csg's","csr","csv",
			"cucumber","cuda","culminating","cultivate","cultivating","cultural","culture",
			"curate","curator","currency","current","currently","curricula","curriculum",
			"custom","customer","customer's","customers","customers'","customersneeds","customizable",
			"customized","customizes","cutting","cybertrans","cycle","cycles","d",
			"d3","daily","darpa","dashboards","data","datacenters","dataflow",
			"dataset","datastax","datastore","date","dates","day","days",
			"db","db2","dba","dbms","dda","de","deadline",
			"deadlines","deal","dealing","debate","debug","debugs","decide",
			"deciphering","decision","decisions","decommission","decommissioning","decompose","decomposition",
			"deconflict","deconstruct","deconstruction","decryption","dedicated","deep","defect",
			"defects","defend","defense","defenses","deficiencies","define","defined",
			"defines","defining","definite","definition","definitions","degradation","degree",
			"degrees","delegating","delete","deleting","deliver","deliverable","deliverables",
			"delivered","deliveries","delivering","delivers","delivery","demand","demanding",
			"demands","demo","demonstrates","demonstrating","demonstration","demonstrations","density",
			"depart","departments","dependable","dependencies","dependency","dependent","depending",
			"depends","depictions","deploy","deployed","deploying","deployments","deploys",
			"depth","deputy","derive","derived","deriving","describe","describes",
			"description","descriptions","designated","designed","designer","designers","designing",
			"designs","desirable","desire","desired","desires","desk","desks",
			"desktop","desktops","detail","detailboth","detailed","detailing","details",
			"detect","detecting","detection","determine","determined","determines","determining",
			"developers","development","developments","develops","deviance","device","devices",
			"devise","dfs","dhtml","diagnose","diagnosing","diagnosis","diagnostic",
			"diagrams","dialogs","dialogue","dictionaries","dictionary","different","differing",
			"difficult","difficulty","digital","diligence","diplomacy","direct","directed",
			"directing","direction","directions","directive","directives","directly","director",
			"directorate","directorates","directors","directory","directs","dirichlet","disagreements",
			"disambiguation","disaster","disburses","discern","discerning","disciplinary","discipline",
			"disciplined","disciplines","disconnected","discover","discoverability","discovered","discovery",
			"discrepancies","discrepancy","discretion","discuss","discussed","discussion","discussions",
			"disguises","disks","disparate","display","displayed","disposal","disruption",
			"disruptions","disruptive","disseminated","disseminating","dissemination","distill","distilling",
			"distinct","distinguish","distinguishing","distribute","distributed","distributing","distribution",
			"distributions","dive","diverse","diversity","dives","division","division's",
			"divisions","dl","dll","dlp","dns","do","docker",
			"docs","document","documentation","documented","documenting","documents","documents'",
			"docusign","dod","does","doing","domain","domains","domestic",
			"domestically","domino","done","down","downstream","downtime","dozens",
			"dr","draft","drafting","drafts","dramatically","draw","drawing",
			"drawings","drive","driven","driver","drivers","drives","driving",
			"drm","drools","drop","drs","dtd","dud","due",
			"duplicating","duplication","duplicative","during","duties","duty","dvds",
			"dynamic","dynamics","dynamodb","e","ea","each","eagerness",
			"eam","eap","ear","earliest","early","earned","ease",
			"easier","easily","easy","ec","ec2","echosign","ecl",
			"eclipse","economic","economical","economics","ed","edge","edismax",
			"edit","editorial","editors","edits","educate","education","educational",
			"educator","ee","effect","effective","effectively","effectiveness","efficiencies",
			"efficiency","efficient","efficiently","effort","efforts","eg","eight",
			"either","elastic","elasticsearch","elearning","electing","electronic","elegant",
			"element","elements","elevations","eleven","elicit","elicitation","eliciting",
			"eliminate","eliminated","eliminating","else","email","emails","embedded",
			"ember","emc","emergency","emergent","emphasis","employ","employed",
			"employee","employees","employing","empted","emulate","enable","enables",
			"enabling","encase","enclave","enclaves","encompass","encompasses","encompassing",
			"encouraged","encrypting","encryption","end","endeavors","endpoints","endure",
			"energy","enforce","enforcement","enforcing","engage","engaged","engagement",
			"engagements","engages","engaging","engine","engineer's","engineers","engineersenior",
			"engines","english","enhance","enhanced","enhancement","enhancements","enhances",
			"enhancing","enjoys","enough","enrich","enriched","enrichment","ensuing",
			"ensure","entail","entailed","entails","enterprise","enterprises","enters",
			"entire","entirely","entities","entitlement","entity","entry","environment",
			"environmental","environments","envision","epic","epics","epo","epolicy",
			"equally","equipment","equities","equivalent","er","erbs","erecruiting",
			"error","errors","erwin","escalation","especially","esri","essential",
			"establish","established","establishes","establishing","establishment","estimate","estimated",
			"estimates","estimating","estimation","esx","esxi","etc","ethernet",
			"etl","evaluate","evaluated","evaluates","evaluating","evaluation","evaluations",
			"even","event","events","eventually","every","evidence","evm",
			"evolution","evolutionary","evolve","evolving","ex","examine","examining",
			"example","examples","exceed","excellence","excellent","exceptional","exceptionally",
			"exchange","exchanges","exciting","executable","executables","execute","executed",
			"executes","executing","execution","executive","executives","exemplars","exemplary",
			"exercise","exercises","exert","exhibit","exhibiting","exist","existence",
			"existing","exists","exits","expand","expanding","expands","expect",
			"expectation","expectations","expected","expediently","expedited","expedites","expeditiously",
			"expenses","experience","experiences","expert","expertise","experts","explain",
			"explaining","explanations","exploit","exploitation","exploiting","exploits","exploration",
			"explore","explorer","exploring","exponential","exposed","exposing","exposure",
			"expressing","expression","ext","extend","extended","extending","extends",
			"extendscript","extensible","extension","extensions","extensive","extent","exterior",
			"external","externalized","externally","extjs","extract","extracted","extracting",
			"extraction","extractions","extrapolate","extreme","extremely","f","fabric",
			"fabrics","face","faceted","facets","facilitate","facilitated","facilitates",
			"facilitating","facilitation","facilities","facility","facing","factors","facts",
			"failover","failure","failures","false","familiar","familiarity","familiarization",
			"faq","faqs","far","fashion","fast","faster","fault",
			"fdrb","feasibility","feasible","feature","features","fed","federate",
			"feed","feedback","feeds","fellow","few","fewer","fi",
			"fiber","fidelity","field","fields","file","files","fill",
			"filling","filtering","final","finalize","finally","finance","finances",
			"financial","financials","find","finding","findings","finely","finish",
			"finished","firebug","firefox","firesight","firewall","firewalls","firmware",
			"first","fit","fits","five","fix","fixes","flagship",
			"flash","fledgling","flex","flexibility","flexible","flight","floor",
			"flow","flows","flyers","focal","focus","focused","focusing",
			"folder","folders","follow","followed","following","footprints","for",
			"forces","forecasting","foreign","foreman","forest","forests","forge",
			"form","formal","formalizing","format","formation","formats","formatted",
			"formatting","forms","formulas","formulate","formulating","formulation","forum",
			"forums","forward","foster","fostering","found","foundation","foundational",
			"foundations","founded","four","fp","frames","framing","frequency",
			"frequent","frequently","friendly","from","front","fsp","fsps",
			"ftk","ftp","fulfill","fulfilling","fulfillment","full","fully",
			"function","functional","functionality","functionally","functioning","functions","fundamental",
			"funding","further","fuse","future","fy","fydp","g",
			"gain","gained","game","ganglia","gantt","gap","gaps",
			"gate","gates","gateways","gather","gathering","gathers","gazebo",
			"gbsd","gdb","gen","gen8","general","generalist","generalized",
			"generally","generate","generated","generates","generating","generation","generic",
			"geospatial","get","getting","gib","gis","give","given",
			"gives","giving","global","globally","globe","glue","go",
			"goal","goals","goes","going","good","google","governance",
			"governing","government","grammar","granting","graph","graphic","graphical",
			"graphs","grasp","grave","great","greater","greenplum","grep",
			"groom","grooming","ground","group","group's","grouping","groups",
			"grow","growing","grows","growth","gtm","guard","guidance",
			"guide","guided","guideline","guidelines","guides","guiding","guis",
			"gumstix","h","ha","hack","hackers","had","hand",
			"handle","handling","handouts","hands","hard","hardcopy","harden",
			"hardening","harness","harnesses","has","hash","hat","have",
			"having","hbase","hc","hdfs","hds","he","headquarters",
			"healing","health","heavily","heavy","help","helping","helps",
			"her","hercules","heterogeneous","hiberate","high","highcharts","higher",
			"highest","highlights","highly","him","hiring","his","historical",
			"history","hitachi","hive","hoc","hold","holding","holdings",
			"holds","holidays","holistic","horizontal","hortonworks","host","hosted",
			"hot","hour","hours","house","housed","how","however",
			"hpc","hql","hr","httpd","hub","hudson","human",
			"hundreds","hurdles","hw","hybrid","hyper","hypertext","hyperv",
			"hypervisor","hypervisors","hypotheses","i","ia","iarpa","ibatis",
			"ibm","ibpm","ic","icds","icons","ide","idea",
			"ideal","ideally","ideas","ideate","ideation","identifiable","identification",
			"identified","identifies","identify","identifying","identity","ie","if",
			"iis","ill","illustrate","illustrated","illustrations","illustrator","ilo",
			"images","imaging","imagining","immediate","immense","imo","impact",
			"impacted","impacting","impacts","impala","impede","impediments","imperative",
			"implement","implementable","implementation","implementations","implemented","implementers","implements",
			"import","importance","important","improve","improved","improvement","improvements",
			"imto","in","inadequate","incentive","inception","incident","incidents",
			"include","included","includes","including","incoming","incompatibilities","inconsistencies",
			"inconsistent","incorporate","incorporates","incorporating","incorporation","increase","increased",
			"increases","increasing","increasingly","incremental","incumbent","independent","independently",
			"indepth","indesign","index","indexes","indexing","indicated","indicators",
			"indices","indictors","individual","individually","individuals","industry","inefficiencies",
			"inefficient","influence","influencing","inform","informal","information","informed",
			"informing","infosec","infrastructure","infrastructures","infrequent","infrequently","infuse",
			"ingest","ingested","ingesting","ingestion","inherited","initial","initialize",
			"initially","initiate","initiated","initiates","initiative","initiatives","injecting",
			"inner","innocentive","innovation","innovations","innovative","input","inputs",
			"inquiries","insert","inserted","inserting","inside","insider","insight",
			"insights","inspect","inspecting","inspections","inspiring","install","installation",
			"installations","installers","installing","installs","instance","instances","instant",
			"instead","institute","instruction","instructional","instructions","instructor","instructors",
			"insuring","integral","integrate","integrated","integrates","integrating","integrations",
			"integrator","integrators","integrity","intelligence","intended","intensive","intent",
			"inter","interact","interacting","interaction","interactions","interactive","interacts",
			"interconnected","interconnectivity","interdependent","interest","interested","interests","interface",
			"interfaces","interfacing","intermittent","internal","internally","international","internationally",
			"internet","interoperability","interoperable","interpersonal","interplay","interpret","interpretation",
			"interpreted","interpreting","interrelates","interrelationships","interval","interventions","interviewing",
			"interviews","intimately","into","intra","intranet","intranets","introduce",
			"introduced","introducing","introduction","intrusion","intuitive","inventory","investigate",
			"investigates","investigating","investigation","investigations","investigators","investment","investments",
			"invited","involve","involved","involvement","involves","involving","ip",
			"ipads","ipsec","iptables","ipv","ir","irm","is",
			"iscm","isconfigurations","isd","ise","isolate","isolated","isolating",
			"isolation","isp's","isse","issetup","issm","isso","issue",
			"issues","issystem","it","item","items","iteration","iterations",
			"iterative","iteratively","itil","its","itself","itsm","iv&v",
			"j","j2ee","jack","jackrabbit","jargon","jaws","jboss",
			"jbpm","jbuilder","jdbc","jdk","jee","jersey","jms",
			"job","jobexperience","jobs","join","joins","joint","joomla",
			"jpa","js","jsp","jsps","judge","judgment","julia",
			"jump","jumpstart","junior","juniper","junos","just","justification",
			"justify","jvm","kafka","kanban","kapow","keep","keeping",
			"keeps","kept","kerberos","kernel","kernels","key","keytool",
			"kibana","kickstart","kickstarter","kirkpatrick","km","kmg","kml",
			"know","knowledge","knowledgeable","known","ksas","kvm","lab",
			"labor","laboratory","lamp","lan","landing","landscape","language",
			"languages","lans","laptop","laptops","large","larger","largescale",
			"last","lasting","lastly","latency","latent","later","latest",
			"latex","latin","launch","launching","law","laws","layer",
			"layered","layers","laying","layout","layouts","lays","lbs",
			"lda","ldap","ldaps","le","lead","leader","leaders",
			"leadership","leading","leads","lean","leanagile","leaning","learn",
			"learnable","learned","learner","learners","learning","least","lecture",
			"led","legacy","legacys","legal","less","lesson","lessons",
			"level","levels","leverage","leveraged","leverages","leveraging","lexis",
			"lexisnexis","liaison","library","license","life","lifecycle","lifecycles",
			"liferay","like","likely","likewise","limit","limitations","limited",
			"limits","lines","linguist","linguistic","link","linking","lisp",
			"list","listed","listen","listening","lists","literature","little",
			"live","load","loaded","loading","loads","local","locally",
			"locate","location","locations","lockdown","lockouts","log","log4j",
			"logging","logic","logical","logically","logistic","logistical","logistics",
			"logos","logs","logstash","long","longer","longitudinal","look",
			"looking","loop","loopholes","loops","lotus","low","lsi",
			"lucene","luns","lvm","lync","m","mac","machine",
			"machines","macintosh","macros","made","madlib","mahout","mail",
			"mainly","maintain","maintainability","maintained","maintaining","maintains","maintenance",
			"major","make","makers","makes","making","malfunctions","man",
			"manage","manageable","managed","management","manager","managerial","managers",
			"manages","managing","mandated","mandates","mandatory","manger","mangers",
			"manipulate","manipulated","manipulating","manipulation","manner","manning","manual",
			"manually","manuals","many","map","mapboxjs","mapnik","mappings",
			"mapreduce","maps","mark","market","marketing","markup","marshalling",
			"mass","massively","material","materials","math","mathematical","mathematics",
			"matrices","matrix","matrixed","matter","matters","mature","matures",
			"maturity","maximize","maximizes","maximo","maximum","may","mcafee",
			"mdm","meaning","meaningful","means","measurable","measure","measurement",
			"measures","measuring","mechanics","mechanisms","media","mediated","mediawiki",
			"medical","medically","medium","medium'scale","meet","meeting","meetings",
			"meets","member","members","members'","membership","memcache","memo",
			"memoranda","memorandum","memorandums","memory","memos","mentioned","mentor",
			"mentoring","menu","merging","meshlab","message","messages","messaging",
			"met","meta","metadata","metal","method","methodological","methodologies",
			"methodology","methods","metric","metrics","metropolitan","micro","microcircuits",
			"microservices","microsoft","microsofts","mid","midand","middle","middleware",
			"might","migrate","migrated","migrating","migration","migrations","milestone",
			"milestones","million","minded","mine","minimal","minimizing","minimum",
			"mining","minor","minutes","mirroring","mis","misattributed","misconfiguration",
			"missing","mission","mission's","missions","misunderstandings","misuse","mitigate",
			"mitigating","mitigation","mitigations","mix","mixed","mixture","ml",
			"moa","moas","mobile","mobility","mobilization","mock","mockup",
			"mockups","modalities","model","modeler","modeling","modelling","models",
			"moderate","moderately","modern","modernization","modernize","modernized","modifiable",
			"modification","modifications","modifies","modify","modifying","modular","modularization",
			"module","modules","momentum","monetary","mongodb","mongrel","monitor",
			"monitored","monitoring","monitors","monthly","months","moodle","more",
			"most","motion","motivated","motivator","mou","move","moves",
			"moving","mpi","mpio","mq","mssql","msvisio","mts",
			"much","mudbox","multi","multidisciplinary","multifaceted","multifactor","multifunctional",
			"multilingual","multimedia","multipath","multiple","multisource","multitask","multitasking",
			"must","mutual","mvc","mvi's","my","mybatis","myriad",
			"n","name","names","narrative","nas","nat","nating",
			"national","native","natural","nature","navigate","navigating","navigation",
			"ndas","near","necessary","necessitates","necessity","need","needed",
			"needs","negative","negotiate","negotiating","negotiation","negotiations","nervous",
			"nessus","net","netapp","netbackup","netbeans","netezza","network",
			"networked","networking","networks","neural","new","newer","newest",
			"newly","newsletter","newsletters","nexis","next","nfs","niagarafiles",
			"night","nights","nine","nittf","nix","nmap","no",
			"nodejs","nodes","noisy","non","non'standard","nontechnical","norm",
			"normal","normalized","not","notation","note","notebook","noted",
			"notes","nothing","notice","notices","notification","noting","noun",
			"novel","novelty","novo","now","ntp","number","numerous",
			"nvd3","nxos","o","o&m","object","objective","objectives",
			"objects","obligations","obscure","observations","observe","obstacles","obtain",
			"obtained","obtaining","occasion","occasional","occasionally","occasions","occupational",
			"occur","occurring","occurs","odd","odnoklassniki","of","off",
			"offensive","offer","offered","offering","offerings","offers","office's",
			"officer","officer's","officers","offices","official","officials","offline",
			"offsite","offsites","often","oftentimes","old","older","olympic",
			"oms","on","onboard","onboarding","once","one","ones",
			"ongoing","online","only","onsite","ontologies","ontology","open",
			"opengeo","openmp","opensource","openstack","openstreetmap","openview","openvpn",
			"operability","operate","operates","operating","operation","operationalize","operationalizing",
			"operationally","operations","operators","opinions","opportunities","opportunity","ops",
			"optic","optimal","optimally","optimizations","optimizing","optimum","optional",
			"options","or","oral","orally","orchestrate","orchestrating","orchestration",
			"orchestrator","order","ordering","organization","organization's","organizational","organizations",
			"organize","organized","organizing","orientation","oriented","original","originators",
			"orm","orrs","os","osi","osp","osps","osx",
			"other","others","otherwise","our","out","outage","outages",
			"outcome","outcomes","outgoing","outlines","outlining","outlook","output",
			"outputs","outreach","outside","outstanding","over","overall","overarching",
			"overcome","overseas","oversee","overseeing","oversees","oversight","oversights",
			"overtime","overviews","owl","own","owner","owners","ownership",
			"owning","p","paas","pace","paced","pack","package",
			"packaged","packages","packaging","packet","packing","page","pager",
			"pages","pair","pairs","palantir","palns","panel","paper",
			"paperless","papers","paradigms","parallel","parameters","parent","parse",
			"parsers","parsing","part","participant","participants","participate","participates",
			"participating","participation","participatory","participle","particular","particularly","parties",
			"partitioning","partner","partner's","partnered","partnering","partners","partners'",
			"partnership","partnerships","parts","party","passenger","passionate","password",
			"passwords","past","patch","patches","patching","path","pathing",
			"paths","pathways","pattern","patterns","pave","pays","pdb",
			"pdus","peak","peculiar","peer","peers","pending","pentaho",
			"people","performance","performer","perimeters","period","periodic","periods",
			"peripherals","perl","permission","permissions","permit","permits","persist",
			"persistence","person","persona","personable","personal","personally","personnel",
			"persons","perspectives","pertaining","pertains","pertinent","pfsense","phase",
			"phased","phenomenology","phone","phones","photo","photos","physical",
			"physically","pi","pick","picture","pig","pilot","piloting",
			"pilots","ping","pipeline","pipelines","pitch","piv","pki",
			"place","placed","placemats","places","plan","planned","planning",
			"plans","platform","platforms","play","played","player","players",
			"plays","please","plugin","plugins","plus","pmbok","pmf",
			"pmo","pms","poa&m","poam","poams","poc","point",
			"pointing","points","polices","policies","policy","policymakers","policyto",
			"pooled","popular","population","port","portable","portal","portals",
			"portfolio","portfolio's","portfolios","porting","portion","portions","portlet",
			"position","positions","positive","positively","positives","possess","possessing",
			"possibilities","possible","possibly","post","posted","posters","postgis",
			"postgres","postgresql","postgressql","posting","postresql","postrgres","posts",
			"posture","potential","potentially","power","powered","powerful","powerpath",
			"powerpc","practical","practice","practices","practicing","prb","pre",
			"precise","predict","prediction","predictive","predominantly","preferably","preference",
			"preferred","preform","preliminary","premier","premise","preparation","prepare",
			"prepared","prepares","preparing","prepublication","presence","present","presentation",
			"presentational","presentations","presented","presenters","presenting","preserved","pressure",
			"pressures","prevailing","prevent","prevents","previous","price","prices",
			"pricing","primarily","primary","primavera","prime","principal","principally",
			"principles","print","printed","printer","printers","printing","prior",
			"priorities","prioritization","prioritize","prioritized","prioritizing","priority","privacy",
			"private","privilege","privileged","privileges","privoxy","prize","prizes",
			"pro","proactive","proactively","probability","problem","problem'solving","problems",
			"procedural","procedure","procedures","proceeds","process","processed","processes",
			"processing","processors","procure","procurement","procurements","procuring","prod",
			"produce","produced","produces","producing","product","production","productive",
			"productively","productivity","productize","products","profession","professional","professionally",
			"professionals","proficiency","proficient","proficiently","profile","profiled","profiles",
			"profiling","program","program's","programing","programmatic","programmatically","programmatics",
			"programmer","programmers","programmers'","programming","programs","progress","progresses",
			"progression","progressive","progressively","project","project's","projected","projecting",
			"projections","projectors","projects","proliant","promise","promising","promote",
			"promotes","promoting","promotion","promotional","proof","proofs","propagation",
			"proper","properly","property","proposal","proposals","propose","proposed",
			"proposes","proposing","proprietary","prospective","protect","protected","protecting",
			"protection","protocol","protocols","prototype","prototypes","prototyping","protractor",
			"proven","provide","provided","provider","providers","provides","providing",
			"provision","provisioned","provisioning","proxy","ps","psychology","public",
			"publication","publications","publicize","publish","publishable","publishing","pulled",
			"pump","punctuation","purchase","purpose","purposes","pursuit","purview",
			"push","pushing","put","pxe","q","qa","qc",
			"qemu","qrc","quad","qualifications","qualified","quality","qualtrics",
			"quantitative","quarterly","queries","query","querying","question","questionnaire",
			"questionnaires","questions","queue","queues","quick","quickly","r",
			"r&d","r2","r2+","r2d","rac","rack","radio",
			"raid","raided","rails","raise","raises","raising","random",
			"range","ranges","ranging","rapid","rapidly","rapport","rates",
			"rational","rationale","raw","rbac","rdbms","rdf","rds",
			"re","reach","reaching","react","reaction","reactivation","read",
			"readable","reader","readiness","reading","ready","real","realistic",
			"reality","realization","realize","realizing","reasonable","reasoned","reboots",
			"rebuild","receipt","receive","received","receiving","recent","recently",
			"reciprocity","recognition","recommend","recommendation","recommendations","recommended","recommending",
			"recommends","recon","reconstruction","record","recorder","recorders","recording",
			"recordresponsible","records","recover","recovering","recovery","recreate","recruitment",
			"recurrent","recurring","red","redeployment","redesign","redesigned","redshift",
			"reduce","reducing","reduction","redundant","refactor","refactoring","refer",
			"reference","referenced","references","referent","refine","refinement","refinements",
			"refines","refining","reflect","reflected","reflective","reflects","regard",
			"regarding","regardless","regards","regime","regimes","registered","registers",
			"registrars","regression","regular","regularize","regularly","regulations","regulatory",
			"rehabilitate","rehosting","reinforce","reinvests","relate","related","relates",
			"relating","relation","relations","relationship","relationships","relative","relatively",
			"release","released","releases","relevance","relevant","reliability","reliable",
			"relied","relies","relocation","rely","remaining","remains","remediation",
			"remedies","remedy","reminders","remote","remotely","removable","removal",
			"remove","removing","render","rendering","renewals","reorganization","repair",
			"repeatability","repeatable","replace","replacement","replacements","replacing","replicability",
			"replication","replies","repo","report","reported","reporting","reports",
			"repositories","repository","represent","representation","representational","representations","representative",
			"representatives","representing","represents","reps","repurposing","reputation","request",
			"requested","requesting","requests","require","required","requirement","requirements",
			"requires","requiring","research","researches","researching","researchs","reset",
			"resetting","reside","residing","residual","resiliency","resistance","resolution",
			"resolutions","resolve","resolved","resolves","resolving","resource","resourceful",
			"resources","resourcing","respect","respective","respectively","respond","responded",
			"responding","responds","response","responses","responsibilities","responsibility","responsible",
			"responsive","responsiveness","restoration","restore","restores","restoring","restrictions",
			"result","resulted","resulting","results","retests","retirement","retouching",
			"retrieval","retrieve","retrieving","retrospective","retrospectives","return","returning",
			"reuse","revamp","revamping","revealing","reverse","review","reviewed",
			"reviewers","reviewing","reviews","revise","revises","revising","revision",
			"revisions","revoking","revolution","revolutionize","reward","rewarding","rf",
			"rfis","rhel","rhev","rhythm","rhythms","rich","richfaces",
			"right","rights","rigid","rigor","rigorous","risks","rman",
			"road","roadmap","robot","robotic","robust","roi","role",
			"roles","roll","rollout","room","rooms","root","ros",
			"rotating","rotation","rotational","rounds","route","router","routers",
			"routine","routinely","routines","routing","rpms","rspec","rss",
			"rsyslog","rub","rule","rules","run","running","runs",
			"runtime","runtimes","rvtm","rvtms","s","sa","saas",
			"safe","safeguard","safeguarding","safeguards","safety","sailsjs","same",
			"saml","sample","san","sanctioned","sandbox","sans","sas",
			"satellite","satisfaction","satisfactorily","satisfied","satisfy","satisfying","saved",
			"savings","say","sca","scalability","scalable","scale","scaled",
			"scaling","scan","scanners","scanning","scans","sccd","sccm",
			"scenario","scenarios","schedule","scheduled","scheduler","schedules","scheduling",
			"schema","schemas","schematics","schemes","scholarly","school","science",
			"sciences","scientist","scientists","scm","scom","scope","scorm",
			"scp","screen","script","scriptaculous","scripts","sctm","sculptor",
			"sculptural","sdk","sdks","sdlc","se","se's","seam",
			"seamless","seamlessly","search","searches","seasoned","secondarily","section",
			"sector","secure","secured","securely","securing","see","seek",
			"seeking","seeks","seem","seen","segment","segmentation","segmented",
			"segments","select","selected","selectee","selecting","selection","selections",
			"self","selinux","semantec","semantic","semantics","semi","seminars",
			"send","senior","seniors","sensitive","sensitivity","sensors","separate",
			"separated","separation","sequences","series","serve","servelts","server",
			"servers","serves","service","services","servicing","serving","servlets",
			"session","sessions","set","sets","setting","settings","setup",
			"seven","several","severity","sh","shadowlink","shape","shaping",
			"share","sharing","sharp","she","sheet","sheets","shelf",
			"shepherding","shift","shifting","shifts","shoes","shoot","shooting",
			"short","shortcomings","shortfalls","should","show","showcase","showing",
			"shows","si","side","sidewinder","sigma","sign","signage",
			"signal","signature","signatures","signed","significance","significant","significantly",
			"simple","simplify","simply","simulated","simulating","simulations","simultaneous",
			"simultaneously","sina","since","single","singular","site","sites",
			"situations","six","size","sized","sizing","skill","skilled",
			"skillfully","skills","skillset","sl","sla","slas","slave",
			"slides","slm","slq","sm","small","smart","sme",
			"sme's","smes","smooth","smoothly","sms","sns","so",
			"soa","social","socialized","sociology","socket","soft","solar",
			"solaris","solarwinds","sole","solicit","solicitation","soliciting","solid",
			"solr","solrcloud","solutions","solve","solves","some","someone",
			"sonet","sop","sophisticated","sops","sorting","sound","source",
			"sourced","sourcefire","sources","space","spacewalk","spacing","span",
			"spanning","spark","speak","speakers","speaking","speaks","spearhead",
			"spearheading","special","specialist","specialists","specialized","specializes","specializing",
			"specific","specifically","specification","specifications","specified","specify","spectrum",
			"speed","spend","spills","sponsor","sponsor's","sponsored","sponsoring",
			"sponsors","sponsors'","spot","spread","spreadsheet","spreadsheets","springs",
			"sprint","sprints","spss","sqldeveloper","sqlite","sqlloader","sqs",
			"sr","srd","srs","ssh","ssl","ssm","sso",
			"ssp","ssps","ssrs","stability","stabilization","stack","stacks",
			"staff","staffs","stage","stages","staging","stake","stakeholder",
			"stakeholders","stand","standalone","standard","standardization","standardize","standardized",
			"standards","standing","standpoint","standup","start","starter","starting",
			"stata","state","stated","stateless","statements","states","static",
			"stations","statistical","statistics","status","statuses","stay","staying",
			"stays","ste","step","steps","steward","stewards","stewardship",
			"stigs","still","stopping","storage","store","stored","stores",
			"stories","storing","story","straightforward","strategic","strategically","strategies",
			"strategy","stream","streamline","streamlined","streams","streamsets","strengthen",
			"stress","strict","strictly","string","strings","strive","strong",
			"structural","structure","structured","structures","structuring","strut","struts",
			"sts","student","students","studies","studio","study","studying",
			"style","styles","stylesheet","sub","subcontractor","subject","subjects",
			"submission","submissions","submit","submitted","submitting","subnets","subordinates",
			"subprogram","subsequent","substance","substantial","substantive","substituted","subsystems",
			"succeed","success","successes","successful","successfully","succinct","such",
			"sufficient","suggest","suggested","suggesting","suggestions","suitability","suitable",
			"suite","suites","summaries","summarization","summarize","summarizing","summary",
			"summer","sun","super","superb","superior","superset","supervised",
			"supervisor","supervisors","supervisory","supply","support","supportable","supported",
			"supporting","supportive","supports","sure","surfaces","surfacing","surge",
			"surrounding","survey","surveys","sustain","sustainable","sustained","sustaining",
			"sustainment","svg","svn","svtc","sw","switch","switched",
			"switches","switching","sybase","sync","synchronize","synopses","synthesis",
			"synthesize","syslog","system","systematic","systematically","systems","systems'",
			"systran","t","t&i","table","tableau","tables","tablets",
			"tactical","tactics","tag","tagging","taglibs","tags","tailor",
			"tailored","tailoring","take","taken","taking","talent","talented",
			"talk","talkabout","talking","tangible","tape","target","targeted",
			"targeter","targeters","targeting","task","tasked","tasking","taskings",
			"tasks","taught","taxonomy","tb","tcps","tdd","tde",
			"te","teaching","team","team's","teambuilding","teamed","teaming",
			"teammates","teamplay","teams","teamwork","tech","technical","technically",
			"technicians","techniques","technological","technologies","technologists","technology","tek",
			"tel","telecommunications","teleconferencing","telephone","telephony","tell","tem",
			"templates","tempo","temporary","tems","ten","tenable","tenants",
			"terabytes","term","terminal","termination","terminology","terms","terrestrial",
			"test","testability","testable","tested","testers","tests","text",
			"textual","than","that","the","their","them","then",
			"theoretical","theories","theory","there","therefore","thereof","theres",
			"these","they","thick","thin","things","think","thinking",
			"third","thirteen","this","thons","thorough","thoroughness","those",
			"though","thought","thoughts","thousands","threaded","threading","threads",
			"threat","threats","three","thresholds","thrives","through","throughout",
			"throughput","throughs","thumb","thus","ticket","ticketing","tickets",
			"ticketswhich","tied","tier","tier2","tiered","tiers","tight",
			"tightly","tim","time","timeframe","timeline","timelines","timely",
			"timeouts","times","tls","tm","tm1","to","today",
			"together","token","tolerances","tolerant","tool","toolkit","tools",
			"top","topic","topics","topologies","total","toward","towards",
			"traceability","tracing","track","tracked","tracker","tracking","tracks",
			"trade","tradecraft","tradeoff","tradeoffs","trades","traditional","traffic",
			"trail","train","trained","trainers","training","trainingclassroom","traits",
			"transaction","transactions","transfer","transferring","transfers","transform","transformation",
			"transformational","transformations","transforming","transit","transition","transitioned","transitioning",
			"translate","translated","translates","translating","translation","translations","transliteration",
			"transport","travel","travelers","traveling","trend","trends","triage",
			"triaging","trial","triggers","trip","trouble","troubleshoots","trr",
			"truly","trust","trusted","truth","ttl","tune","tuning",
			"tunneling","turnaround","turning","tutorials","twelve","twister","two",
			"type","types","typical","typically","u","uam","uat",
			"ubunto","ubuntu","ucs","ui","uis","ultimate","ultimately",
			"ultravnc","uml","unaware","unclassified","uncover","uncovered","under",
			"undergoing","undergraduate","underlie","underlying","understand","understandable","understanding",
			"understands","understood","underway","unified","unique","unit","unit's",
			"units","unless","unlocking","unmarshalling","unpopular","unprecedented","unrelated",
			"unstructured","unsupervised","up","upcoming","update","updated","updates",
			"updating","upgrade","upgraded","upgrades","upgrading","upkeep","upon",
			"upper","ups","upss","uptime","us","usability","usable",
			"usage","use","used","useful","usefulness","user","users",
			"uses","usg","using","utilities","utility","utilization","utilize",
			"utilized","utilizes","utilizing","ux","v","v1","v2",
			"v3","v6","vague","valgrind","valid","validate","validated",
			"validates","validating","validation","validations","validity","valuable","value",
			"valued","values","valve","variables","variances","variants","varied",
			"variety","various","varying","vast","vba","vcenter","vcops",
			"vdi","vdp","vector","vehicles","velocity","vendor","vendors",
			"venues","verbal","verbally","verifiable","verification","verified","verify",
			"verifying","verisign","versatile","version","versions","versus","vertica",
			"very","vested","vet","vetted","vetting","vi","via",
			"viability","viable","video","videos","view","viewing","views",
			"violations","vip","virtual","virtualbox","virtualized","virus","visibility",
			"visible","visio","vision","visit","visual","visualization","visualizations",
			"visualizing","visually","vital","vkontakte","vm","vms","vmware's",
			"vnx","voice","voip","volume","volumes","vormetric","vpc",
			"vpn","vpns","vrealize","vse","vsphere","vtc","vtcs",
			"vulnerabilities","vulnerability","vyatta","vyos","w","w3c","w3cs",
			"walk","wallet","wallets","wan","wans","want","wants",
			"ware","warehouse","warehouses","warehousing","was","washington","watch",
			"waterfall","wavemaker","way","ways","wbs","we","weakness",
			"weaknesses","web","webcasts","webinspect","weblogic","webserver","webservice",
			"webservices","websites","websphere","week","weekend","weekends","weekly",
			"weeks","weighing","weighting","welcome","well","were","what",
			"when","where","whether","which","while","white","who",
			"whom","whose","why","wi","widget","wikis","will",
			"willing","willingness","win","win7","window","winds","wire",
			"wired","wireframe","with","without","wma","wms","word",
			"work","workbench","workbooks","worked","workers","workflow","workflows",
			"workforce","working","workings","workload","workloads","works","workshops",
			"workspace","workstation","workstations","world","worlds","worldwide","would",
			"write","writer","writes","wrong","ws","wsus","x",
			"x+","x509","xcode","xhtml","xp","xpages","xpath",
			"xqdt","xquery","xry","xsd","xsl","xsml","y",
			"year","yearly","years","years'","yet","you","your",
			"yrs","ystems","yui","yum","zbrush","zenoss","zookeeper",""," "));

	ArrayList<String> keyWords = new ArrayList<String>(Arrays.asList("acrobat","addie","agileframework","ajax","android","angularjs",
			"ant","apache","applicationsdatabase","applicationsdatabases","artifactory","artifacts","auditing",
			"authentication","aws","b.a","b.s","backup","bash","biometric",
			"bootstrap","c","c#","c++","captivate","chef","citrix",
			"cmdb","commandline","continuousdeployment","continuousintegration","cryptography","css","css3",
			"cvs","cybersecurity","ddos","debugging","design","developing","devops",
			"dreamweaver","eclipse","excel","federal","frameworks","git","github",
			"gitlab","governmental","gradle","grails","groovy","gwt","hadoop",
			"hibernate","html","html5","http","ids","importingcertificates","instagram",
			"ios","java","javascript","jenkins","jetty","jquery","json",
			"licensing","linkedin","linux","managment","matlab","maven","mongo",
			"msoffice","mysql","nagios","node","nosql","operationaldatabases","oracle",
			"php","pl","powerpoint","powershell","puppet","putty","python",
			"redhat","redmine","regex","relationaldatabase","relationaldatabases","rest","restful",
			"riskassessments","ruby","s3","scrum","scrummaster","security","selenium",
			"servicenow","sftp","sharepoint","shellscripting","soap","softwareengineering","splunk",
			"spring","sql","striper","subversion","tcp","telecom","testing",
			"tomcat","troubleshooting","twitter","unix","vmware","weaver","web-service",
			"weibo","windows","wireshark","wordpress","xml","xslt"));

	/*
	 * Opens the AWS client with provided access keys
	 * 
	 * @param accessKey
	 * @param secretKey
	 */
	public void init(String accessKey, String secretKey) {
		s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
	}

	/**
	 * Read a character oriented file from S3
	 *
	 * @param bucketName  Name of bucket
	 * @param key         File Name
	 * @param s3g			GUI interface
	 * @throws IOException	If files don't exist
	 */
	public void readFromS3(String bucketName, String key, S3GUI s3g) throws IOException {
		S3Object s3object = s3.getObject(new GetObjectRequest(bucketName, key));
		if(s3object.getObjectMetadata().getContentType().equalsIgnoreCase("application/vnd.ms-excel"))
		{
			//Retrieves file(s) from bucket
			InputStream inp = s3object.getObjectContent();

			//Makes a new .xlsx workbook
			XSSFWorkbook wb = new XSSFWorkbook(inp);

			//Retrieves info in first sheet of workbook
			Sheet sheet1 = wb.getSheetAt(0);

			//Closes input stream as no more data is needed
			inp.close();

			//No more files to retrieve
			s3object.close();

			/*
			 * Goes through each cell and if it is index 7,8 (H,I) grabs the contents if it is String formatted
			 * And passes it to the helper function commonWordRemove(String s) 
			 * And then writes it to an output file and appends it to the string buildUp
			 */
			for (Row row : sheet1) 
			{
				for (int index = 0; index < row.getPhysicalNumberOfCells(); index++) 
				{

					if (index == 7 || index == 8)
					{
						Cell cell = row.getCell(index);

						if (cell != null)
						{

							switch (cell.getCellType())
							{
							case Cell.CELL_TYPE_STRING:
								String temp = cell.getRichStringCellValue().getString();
								String toWrite = "";
								try {
									toWrite = this.commonWordRemove(temp);
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								}
								totalText += (toWrite+" ");
								break; 
							default: 
							}
						}
					}
				}
			}
			wb.close();

			//Stores all the words from buildUp not in commonWords
			ArrayList<Word> words = new ArrayList<>();
			Word word;
			boolean found = false;
			ArrayList<String> splitWords = new ArrayList<String>(Arrays.asList(totalText.split("\\s+")));
			splitWords.removeAll(excludedWords);
			Collections.sort(splitWords);
				for (String w : splitWords)
				{
					word = new Word(w);
					for(int j = 0;j < words.size();j++)
					{
						if(words.get(j).equals(word.getName()))
						{
							words.get(j).addOne();
							found = true;
						}
					}
					if(!found){words.add(word);}
					found = false;
				}
			Collections.sort(keyWords);

			/*
			 * This is used to find buzz words that consist of more than one word. ex Command Line, cyber security, etc
			 * this is done using the two arrays below, the first word goes in the top and matches up with he word on the
			 * bottom. The loop checks every word against the first array and upon finding an instance, it then checks the next
			 * word in line to see if it matches up. 
			 * 
			 * This can be easily added too, add the beginning word into the firsts array and then the second into the other array
			 * from there open the buzzWords file and add the two words, without a space in between them, to the list.
			 */
			String[] firstWords = { "command", "shell", "importing", "risk", "scrum", "software", "cyber", "continuous", "continuous", "ms", "agile", "relational", "relational", "applications", "applications", "operational" };
			String[] secondWords = { "line", "scripting", "certificates", "assessments", "master", "engineering", "security", "integration", "deployment", "office", "framework", "databases", "database", "databases", "database", "databases" };

			for (int i = 0; i < words.size(); i++)
			{
				for (int f = 0; f < firstWords.length; f++)
				{
					Word first = new Word(firstWords[f]);
					Word total = new Word(firstWords[f]+secondWords[f]);
					if (words.get(i).equals(first.getName()))// && splitWords.get(i+1).equals(two.getName()))
					{
						total.setCount(words.get(i).getCount());
						words.add(i, total);
						words.remove(i+1);
						words.remove(i+2);
					}
				}
			}

			s3g.addTextB("KEY WORDS:\n\n");
			//Prints the buzzword and its total count
			for(int i = 0; i < keyWords.size(); i++)
			{	
				for(int j = 0;j < words.size();j++)
				{
					if(words.get(j).equals(keyWords.get(i)))
					{
						s3g.addTextB(keyWords.get(i)+" : "+words.get(j).getCount()+"\n");
						words.remove(j);
					}
				}
			}

			s3g.addTextN("NEW WORDS:\n\n");

			//prints out new words and their total counts
			for(Word w : words)
			{
				s3g.addTextN(w.getName()+" : "+w.getCount()+"\n");
			}

		}
		//This else is if the file(s) in the bucket are not the correct type (excel)
		else
		{
			System.err.println("Incorrect File type in bucket");
			System.err.println("Expected Type: .xlsx");
			s3object.close();
			System.exit(0);
		}
	}

	/**
	 * This method will take in a string to be processed, removes all special characters
	 * then splits it by spaces and adds it to an array list, from there it will use an external list of
	 * common words that it will check for and remove from the array list. after this completes it will then
	 * rebuild the sentence and pass it back. 
	 *
	 * @param s  String to be parsed
	 * @throws FileNotFoundException	File does not exist
	 * @return String without common words
	 */
	public String commonWordRemove(String s) throws FileNotFoundException
	{	  	  
		s = s.replaceAll("[&%[0-9]-()\\/.^:,*]"," ");
		ArrayList<String> line = new ArrayList<>();

		for(String str : s.split("\\s+"))
		{
			line.add(str);
		}

		line.removeAll(excludedWords);

		String temp  = line.toString().toLowerCase();
		temp = temp.replaceAll("[&%[0-9]-()\\/.^:;,*\\[\\]]", " ");
		return temp;
	}

}