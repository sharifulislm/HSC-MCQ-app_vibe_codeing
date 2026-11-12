package com.example.data.catalog

import com.example.data.model.AccentKey
import com.example.data.model.HscChapter
import com.example.data.model.HscMcq
import com.example.data.model.HscStream
import com.example.data.model.HscSubject
import com.example.data.model.McqDifficulty

object HscCurriculumCatalog {

    val subjects: List<HscSubject> = listOf(
        HscSubject(
            id = "ict",
            name = "ICT (Info & Comm. Tech)",
            code = "275",
            stream = HscStream.COMPULSORY,
            description = "Global Village, Networking, Number Systems, HTML, C Programming & DBMS",
            accentKey = AccentKey.INDIGO
        ),
        HscSubject(
            id = "bangla",
            name = "Bangla",
            code = "101-102",
            stream = HscStream.COMPULSORY,
            description = "Sahitya Path (1st Paper) & Bangla Byakaran o Nirmiti (2nd Paper)",
            accentKey = AccentKey.CORAL
        ),
        HscSubject(
            id = "english",
            name = "English",
            code = "107-108",
            stream = HscStream.COMPULSORY,
            description = "English For Today (1st Paper) & Grammar & Composition (2nd Paper)",
            accentKey = AccentKey.TEAL
        ),
        HscSubject(
            id = "physics",
            name = "Physics",
            code = "174-175",
            stream = HscStream.SCIENCE,
            description = "Mechanics, Waves, Thermodynamics, Electromagnetism & Modern Physics",
            accentKey = AccentKey.INDIGO
        ),
        HscSubject(
            id = "chemistry",
            name = "Chemistry",
            code = "176-177",
            stream = HscStream.SCIENCE,
            description = "Atomic Structure, Periodic Trends, Stoichiometry & Organic Chemistry",
            accentKey = AccentKey.EMERALD
        ),
        HscSubject(
            id = "higher_math",
            name = "Higher Mathematics",
            code = "265-266",
            stream = HscStream.SCIENCE,
            description = "Matrices, Calculus, Vectors, Conics, Complex Numbers & Probability",
            accentKey = AccentKey.CORAL
        ),
        HscSubject(
            id = "biology",
            name = "Biology",
            code = "178-179",
            stream = HscStream.SCIENCE,
            description = "Botany (Cell Biology & Genetics) and Zoology (Human Physiology)",
            accentKey = AccentKey.EMERALD
        ),
        HscSubject(
            id = "accounting",
            name = "Accounting",
            code = "253-254",
            stream = HscStream.BUSINESS,
            description = "Double-Entry Accounting, Financial Statements, Company & Cost Accounting",
            accentKey = AccentKey.AMBER
        ),
        HscSubject(
            id = "finance",
            name = "Finance, Banking & Insurance",
            code = "292-293",
            stream = HscStream.BUSINESS,
            description = "Time Value of Money, Capital Budgeting, Banking & Insurance Principles",
            accentKey = AccentKey.TEAL
        ),
        HscSubject(
            id = "management",
            name = "Business Org. & Management",
            code = "277-278",
            stream = HscStream.BUSINESS,
            description = "Business Forms, Entrepreneurship, Planning, Leadership & Controlling",
            accentKey = AccentKey.INDIGO
        ),
        HscSubject(
            id = "economics",
            name = "Economics",
            code = "109-110",
            stream = HscStream.HUMANITIES,
            description = "Microeconomic Theory & Macroeconomics of Bangladesh",
            accentKey = AccentKey.EMERALD
        ),
        HscSubject(
            id = "civics",
            name = "Civics & Good Governance",
            code = "269-270",
            stream = HscStream.HUMANITIES,
            description = "State, Citizenship, Rights & Constitutional History of Bangladesh",
            accentKey = AccentKey.CORAL
        ),
        HscSubject(
            id = "islamic_history",
            name = "Islamic History & Culture",
            code = "267-268",
            stream = HscStream.HUMANITIES,
            description = "Caliphate Era, Umayyad-Abbasid Civilization & Bengal Sultanate",
            accentKey = AccentKey.AMBER
        )
    )

    val chapters: List<HscChapter> = listOf(
        // ICT (6 NCTB Chapters)
        HscChapter("ict_ch1", "ict", "Ch 1: World & Bangladesh Perspective", "ICT Ch-1", "Global Village, Virtual Reality, AI, Robotics, Cryosurgery & Biometrics"),
        HscChapter("ict_ch2", "ict", "Ch 2: Communication Systems & Networking", "ICT Ch-2", "Bandwidth, Guided/Unguided Media, Topologies, Generations & Cloud Computing"),
        HscChapter("ict_ch3", "ict", "Ch 3: Number Systems & Digital Devices", "ICT Ch-3", "Binary/Octal/Hex Conversions, 2's Complement, Logic Gates & Adders"),
        HscChapter("ict_ch4", "ict", "Ch 4: Web Design & HTML", "ICT Ch-4", "URL, IP, DNS, HTML Tags, Hyperlinks, Tables & Website Publishing"),
        HscChapter("ict_ch5", "ict", "Ch 5: Programming Language (C)", "ICT Ch-5", "Algorithms, Flowcharts, Compilers, C Data Types, Loops & Conditionals"),
        HscChapter("ict_ch6", "ict", "Ch 6: Database Management System", "ICT Ch-6", "RDBMS Keys (Primary/Foreign), SQL Queries, Sorting & Data Security"),

        // Bangla
        HscChapter("bangla_1", "bangla", "Bangla 1st Paper (Sahitya Path)", "1st Paper", "Prose (Aparichita, Bilashi, Amar Path) & Poetry (Sirajuddoula, Sonar Tori)"),
        HscChapter("bangla_2", "bangla", "Bangla 2nd Paper (Byakaran)", "2nd Paper", "Uccharon, Banan Bidhi, Samas, Upasarga, Bakya Tattva & Paribhasha"),

        // English
        HscChapter("english_1", "english", "English 1st Paper (English For Today)", "1st Paper", "Reading Comprehension, Vocabulary, Flowcharts & Literary Themes"),
        HscChapter("english_2", "english", "English 2nd Paper (Grammar)", "2nd Paper", "Prepositions, Modifiers, Right Form of Verbs, Narration & Connectors"),

        // Physics
        HscChapter("physics_1", "physics", "Physics 1st Paper", "1st Paper", "Vectors, Motion, Newtonian Mechanics, Work-Energy, Gravitation & SHM"),
        HscChapter("physics_2", "physics", "Physics 2nd Paper", "2nd Paper", "Thermodynamics, Electrostatics, Current Electricity, Optics & Nuclear Physics"),

        // Chemistry
        HscChapter("chemistry_1", "chemistry", "Chemistry 1st Paper", "1st Paper", "Quantum Numbers, Periodic Trends, Hybridization & Chemical Equilibrium"),
        HscChapter("chemistry_2", "chemistry", "Chemistry 2nd Paper", "2nd Paper", "Gas Laws, Organic Reactions, Quantitative Chemistry & Electrochemistry"),

        // Higher Math
        HscChapter("math_1", "higher_math", "Higher Math 1st Paper", "1st Paper", "Matrices, Determinants, Straight Lines, Circles & Differentiation"),
        HscChapter("math_2", "higher_math", "Higher Math 2nd Paper", "2nd Paper", "Complex Numbers, Polynomials, Conics, Statics & Probability"),

        // Biology
        HscChapter("bio_1", "biology", "Biology 1st Paper (Botany)", "1st Paper", "Cell Structure, Mitosis/Meiosis, Microorganisms & Plant Physiology"),
        HscChapter("bio_2", "biology", "Biology 2nd Paper (Zoology)", "2nd Paper", "Animal Diversity, Human Digestion, Circulation & Genetics"),

        // Accounting
        HscChapter("acc_1", "accounting", "Accounting 1st Paper", "1st Paper", "Accounting Equation, Journals, Trial Balance, BRS & Financial Statements"),
        HscChapter("acc_2", "accounting", "Accounting 2nd Paper", "2nd Paper", "Joint Stock Company, Shares, Cash Flow & Cost Sheet Analysis"),

        // Finance
        HscChapter("fin_1", "finance", "Finance 1st Paper", "1st Paper", "Time Value of Money, Risk & Return, Capital Budgeting & Cost of Capital"),
        HscChapter("fin_2", "finance", "Finance 2nd Paper (Banking & Insurance)", "2nd Paper", "Bangladesh Bank, Commercial Banking, Negotiable Instruments & Insurance"),

        // Management
        HscChapter("mgmt_1", "management", "Management 1st Paper", "1st Paper", "Sole Proprietorship, Partnership, Cooperative Societies & State Enterprises"),
        HscChapter("mgmt_2", "management", "Management 2nd Paper", "2nd Paper", "Henri Fayol's 14 Principles, Planning, Staffing, Motivation & Control"),

        // Economics
        HscChapter("econ_1", "economics", "Economics 1st Paper (Micro)", "1st Paper", "Law of Demand & Supply, Elasticity, Utility & Market Equilibrium"),
        HscChapter("econ_2", "economics", "Economics 2nd Paper (Bangladesh)", "2nd Paper", "GDP/GNP, Agriculture, Industrial Policy, Inflation & Foreign Trade"),

        // Civics
        HscChapter("civics_1", "civics", "Civics 1st Paper", "1st Paper", "Elements of State, Good Governance, Law, Liberty & E-Governance"),
        HscChapter("civics_2", "civics", "Civics 2nd Paper", "2nd Paper", "Language Movement 1952, Six-Point Demand, 1971 Liberation & Constitution"),

        // Islamic History
        HscChapter("ih_1", "islamic_history", "Islamic History 1st Paper", "1st Paper", "Pre-Islamic Arabia, Khulafa-e-Rashideen, Umayyads & Abbasid Golden Age"),
        HscChapter("ih_2", "islamic_history", "Islamic History 2nd Paper", "2nd Paper", "Independent Sultanate of Bengal, Mughal Empire & Cultural Heritage")
    )

    val mcqs: List<HscMcq> = listOf(
        // ICT Ch 1
        HscMcq(
            id = "mcq_ict_1_1", subjectId = "ict", chapterId = "ict_ch1",
            question = "Who first introduced the concept of 'Global Village'?",
            options = listOf("Tim Berners-Lee", "Marshall McLuhan", "Charles Babbage", "Mark Zuckerberg"),
            correctIndex = 1,
            explanation = "Canadian philosopher Marshall McLuhan coined the term 'Global Village' in his books The Gutenberg Galaxy (1962) and Understanding Media (1964).",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '23"
        ),
        HscMcq(
            id = "mcq_ict_1_2", subjectId = "ict", chapterId = "ict_ch1",
            question = "Which cryogenic agent is most commonly used in cryosurgery to destroy abnormal tissue?",
            options = listOf("Liquid Oxygen", "Liquid Nitrogen", "Dry Ice", "Helium Gas"),
            correctIndex = 1,
            explanation = "Liquid nitrogen (at approximately -196 degrees Celsius) is the primary cryogen used in cryosurgery to freeze and destroy targeted lesions.",
            difficulty = McqDifficulty.EASY, boardTag = "Rajshahi Board '24"
        ),
        HscMcq(
            id = "mcq_ict_1_3", subjectId = "ict", chapterId = "ict_ch1",
            question = "Which biometric technique identifies a person by analyzing the unique colored ring of the eye?",
            options = listOf("Retina Scan", "Iris Recognition", "Face Geometry", "DNA Profiling"),
            correctIndex = 1,
            explanation = "Iris recognition captures high-contrast images of the colored ring (iris) around the pupil, whereas retina scanning maps blood vessels at the back of the eye.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Cumilla Board '23"
        ),
        // ICT Ch 2
        HscMcq(
            id = "mcq_ict_2_1", subjectId = "ict", chapterId = "ict_ch2",
            question = "In which data transmission mode can data flow in both directions simultaneously?",
            options = listOf("Simplex", "Half-Duplex", "Full-Duplex", "Multicast"),
            correctIndex = 2,
            explanation = "Full-duplex allows simultaneous two-way transmission (e.g., telephone calls), whereas half-duplex permits one direction at a time (e.g., walkie-talkie).",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),
        HscMcq(
            id = "mcq_ict_2_2", subjectId = "ict", chapterId = "ict_ch2",
            question = "Which network topology requires a central hub or switch to connect all nodes?",
            options = listOf("Bus Topology", "Ring Topology", "Star Topology", "Mesh Topology"),
            correctIndex = 2,
            explanation = "In a Star topology, every node connects via a dedicated point-to-point link to a central hub or switch.",
            difficulty = McqDifficulty.EASY, boardTag = "Chattogram Board '24"
        ),
        HscMcq(
            id = "mcq_ict_2_3", subjectId = "ict", chapterId = "ict_ch2",
            question = "What is the standard frequency band used by Bluetooth (IEEE 802.15.1)?",
            options = listOf("900 MHz", "2.4 GHz ISM Band", "5.8 GHz", "60 GHz"),
            correctIndex = 1,
            explanation = "Bluetooth operates in the unlicensed 2.4 GHz ISM (Industrial, Scientific, and Medical) band using Frequency-Hopping Spread Spectrum.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Sylhet Board '23"
        ),
        // ICT Ch 3
        HscMcq(
            id = "mcq_ict_3_1", subjectId = "ict", chapterId = "ict_ch3",
            question = "What is the hexadecimal equivalent of the binary number (10110110)2?",
            options = listOf("A6", "B6", "B5", "C6"),
            correctIndex = 1,
            explanation = "Group binary into 4-bit nibbles: 1011 = 11 = B, and 0110 = 6. Therefore (10110110)2 = (B6)16.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_ict_3_2", subjectId = "ict", chapterId = "ict_ch3",
            question = "Which two logic gates are known as Universal Gates?",
            options = listOf("AND and OR", "NAND and NOR", "XOR and XNOR", "NOT and AND"),
            correctIndex = 1,
            explanation = "NAND and NOR are Universal Gates because any Boolean function or basic logic gate (AND, OR, NOT) can be constructed using only NAND or only NOR gates.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '24"
        ),
        HscMcq(
            id = "mcq_ict_3_3", subjectId = "ict", chapterId = "ict_ch3",
            question = "In a Half Adder circuit, the Carry output is produced by which logic gate?",
            options = listOf("XOR Gate", "AND Gate", "OR Gate", "NAND Gate"),
            correctIndex = 1,
            explanation = "A Half Adder produces Sum = A XOR B and Carry = A AND B.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Jashore Board '23"
        ),
        // ICT Ch 4
        HscMcq(
            id = "mcq_ict_4_1", subjectId = "ict", chapterId = "ict_ch4",
            question = "Which HTML tag is used to create a hyperlink to another webpage?",
            options = listOf("<link>", "<a>", "<href>", "<url>"),
            correctIndex = 1,
            explanation = "The anchor tag <a> with the href attribute defines a hyperlink in HTML, e.g., <a href=\"https://nctb.gov.bd\">NCTB</a>.",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '23"
        ),
        HscMcq(
            id = "mcq_ict_4_2", subjectId = "ict", chapterId = "ict_ch4",
            question = "In an HTML table, which attribute merges two or more adjacent columns horizontally?",
            options = listOf("rowspan", "colspan", "cellspacing", "merge"),
            correctIndex = 1,
            explanation = "The colspan attribute specifies the number of columns a table cell (<td> or <th>) should span across.",
            difficulty = McqDifficulty.EASY, boardTag = "Barishal Board '24"
        ),
        // ICT Ch 5
        HscMcq(
            id = "mcq_ict_5_1", subjectId = "ict", chapterId = "ict_ch5",
            question = "Which format specifier is used in C's printf() to display a double-precision floating-point number?",
            options = listOf("%d", "%f", "%lf", "%c"),
            correctIndex = 2,
            explanation = "%lf is the standard format specifier for double in C (especially in scanf and commonly taught in HSC ICT for double).",
            difficulty = McqDifficulty.EASY, boardTag = "Dinajpur Board '23"
        ),
        HscMcq(
            id = "mcq_ict_5_2", subjectId = "ict", chapterId = "ict_ch5",
            question = "Which loop in C guarantees that the loop body executes at least once before checking the condition?",
            options = listOf("for loop", "while loop", "do-while loop", "nested if"),
            correctIndex = 2,
            explanation = "do-while is an exit-controlled loop: it executes the body first and evaluates the condition at the bottom.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),
        // ICT Ch 6
        HscMcq(
            id = "mcq_ict_6_1", subjectId = "ict", chapterId = "ict_ch6",
            question = "Which database key uniquely identifies each record in a table and cannot contain NULL values?",
            options = listOf("Foreign Key", "Primary Key", "Composite Key", "Candidate Attribute"),
            correctIndex = 1,
            explanation = "A Primary Key enforces entity integrity: every value must be unique and non-null.",
            difficulty = McqDifficulty.EASY, boardTag = "Rajshahi Board '23"
        ),
        HscMcq(
            id = "mcq_ict_6_2", subjectId = "ict", chapterId = "ict_ch6",
            question = "Which SQL statement is used to retrieve data from a relational database table?",
            options = listOf("GET", "SELECT", "EXTRACT", "FETCH"),
            correctIndex = 1,
            explanation = "SELECT is the standard SQL Data Query Language (DQL) command used to query rows from tables.",
            difficulty = McqDifficulty.EASY, boardTag = "Mymensingh Board '24"
        ),

        // Bangla 1st & 2nd Paper
        HscMcq(
            id = "mcq_bn_1_1", subjectId = "bangla", chapterId = "bangla_1",
            question = "Who is the author of the iconic HSC prose story 'Aparichita'?",
            options = listOf("Sarat Chandra Chattopadhyay", "Rabindranath Tagore", "Kazi Nazrul Islam", "Bibhutibhushan Bandyopadhyay"),
            correctIndex = 1,
            explanation = "Rabindranath Tagore wrote 'Aparichita', first published in Sabuj Patra in 1914 (Kartik 1321 BS).",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_bn_1_2", subjectId = "bangla", chapterId = "bangla_1",
            question = "In the poem 'Bidrohi', which river is symbolically invoked by Kazi Nazrul Islam?",
            options = listOf("Padma", "Meghna", "Issamoti", "Ganga"),
            correctIndex = 1,
            explanation = "In 'Bidrohi', Nazrul declares himself as turbulent as the 'Meghna' and 'Israfi's horn' in defiance of oppression.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "NCTB Core"
        ),
        HscMcq(
            id = "mcq_bn_2_1", subjectId = "bangla", chapterId = "bangla_2",
            question = "Which Samas (compound) gives prominence to the meaning of BOTH constituent words (e.g., Ma-Baba)?",
            options = listOf("Tatpurush Samas", "Dwigu Samas", "Dwandwa Samas", "Bahubrihi Samas"),
            correctIndex = 2,
            explanation = "In Dwandwa Samas, both the prior and posterior words carry equal semantic importance.",
            difficulty = McqDifficulty.EASY, boardTag = "Cumilla Board '23"
        ),
        HscMcq(
            id = "mcq_bn_2_2", subjectId = "bangla", chapterId = "bangla_2",
            question = "What is the standard Bangla administrative translation (Paribhasha) for 'Memorandum'?",
            options = listOf("Gopon Nothi", "Smaraklipi", "Adeshnama", "Protipadon"),
            correctIndex = 1,
            explanation = "In HSC Bangla 2nd Paper Paribhasha, 'Memorandum' is translated as 'Smaraklipi' (স্মারকলিপি).",
            difficulty = McqDifficulty.MEDIUM, boardTag = "All Boards '24"
        ),

        // English 1st & 2nd Paper
        HscMcq(
            id = "mcq_en_1_1", subjectId = "english", chapterId = "english_1",
            question = "In HSC English For Today, Nelson Mandela delivered his historic 'rainbow nation' speech after becoming President in which year?",
            options = listOf("1990", "1994", "1996", "1999"),
            correctIndex = 1,
            explanation = "Nelson Mandela became South Africa's first Black president in democratic elections in May 1994.",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '23"
        ),
        HscMcq(
            id = "mcq_en_1_2", subjectId = "english", chapterId = "english_1",
            question = "In 'The Unforgettable History', on which date did Bangabandhu Sheikh Mujibur Rahman deliver the historic 7th March address at Racecourse Maidan?",
            options = listOf("7 March 1970", "7 March 1971", "26 March 1971", "16 December 1971"),
            correctIndex = 1,
            explanation = "The historic speech was delivered on 7 March 1971 at Ramna Race Course Maidan (now Suhrawardy Udyan).",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '24"
        ),
        HscMcq(
            id = "mcq_en_2_1", subjectId = "english", chapterId = "english_2",
            question = "Choose the grammatically correct sentence using 'Scarcely had':",
            options = listOf(
                "Scarcely had we reached the station than the train left.",
                "Scarcely had we reached the station when the train left.",
                "Scarcely we had reached the station when the train left.",
                "Scarcely had we reach the station before the train left."
            ),
            correctIndex = 1,
            explanation = "'Scarcely had + subject + V3' pairs with 'when' (whereas 'No sooner had' pairs with 'than').",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Rajshahi Board '24"
        ),
        HscMcq(
            id = "mcq_en_2_2", subjectId = "english", chapterId = "english_2",
            question = "Identify the correct passive form of: 'Who taught you English?'",
            options = listOf(
                "By whom were you taught English?",
                "By whom was you taught English?",
                "Whom were you taught English by?",
                "Who were you taught English?"
            ),
            correctIndex = 0,
            explanation = "'Who' transforms to 'By whom' at the beginning, followed by auxiliary 'were' agreeing with 'you' + past participle 'taught'.",
            difficulty = McqDifficulty.EASY, boardTag = "NCTB Core"
        ),

        // Physics
        HscMcq(
            id = "mcq_phy_1_1", subjectId = "physics", chapterId = "physics_1",
            question = "If the dot product A . B = 0 for two non-zero vectors A and B, what is the angle between them?",
            options = listOf("0 degrees", "45 degrees", "90 degrees", "180 degrees"),
            correctIndex = 2,
            explanation = "A . B = AB cos(theta) = 0 implies cos(theta) = 0, so theta = 90 degrees (the vectors are perpendicular).",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_phy_1_2", subjectId = "physics", chapterId = "physics_1",
            question = "What is the escape velocity from the surface of Earth (approximately)?",
            options = listOf("7.9 km/s", "9.8 km/s", "11.2 km/s", "14.5 km/s"),
            correctIndex = 2,
            explanation = "Escape velocity v_e = sqrt(2gR) = sqrt(2 * 9.8 * 6.37e6) ≈ 11.2 km/s.",
            difficulty = McqDifficulty.EASY, boardTag = "Cumilla Board '23"
        ),
        HscMcq(
            id = "mcq_phy_2_1", subjectId = "physics", chapterId = "physics_2",
            question = "In an isothermal process of an ideal gas, which thermodynamic quantity remains constant?",
            options = listOf("Pressure", "Volume", "Internal Energy", "Entropy"),
            correctIndex = 2,
            explanation = "In an isothermal process, temperature T is constant. For an ideal gas, internal energy U depends only on T, so dU = 0.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Chattogram Board '24"
        ),
        HscMcq(
            id = "mcq_phy_2_2", subjectId = "physics", chapterId = "physics_2",
            question = "Which phenomenon proves the transverse nature of light waves?",
            options = listOf("Interference", "Diffraction", "Polarization", "Refraction"),
            correctIndex = 2,
            explanation = "Only transverse waves can undergo polarization; longitudinal waves (like sound in air) cannot be polarized.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),

        // Chemistry
        HscMcq(
            id = "mcq_chem_1_1", subjectId = "chemistry", chapterId = "chemistry_1",
            question = "What is the hybridization of the carbon atom in a methane (CH4) molecule?",
            options = listOf("sp", "sp2", "sp3", "dsp2"),
            correctIndex = 2,
            explanation = "Carbon in CH4 forms 4 sigma bonds with zero lone pairs, resulting in sp3 hybridization and tetrahedral geometry (109.5 deg).",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_chem_1_2", subjectId = "chemistry", chapterId = "chemistry_1",
            question = "Which element has the highest electronegativity on the Pauling scale?",
            options = listOf("Oxygen", "Chlorine", "Nitrogen", "Fluorine"),
            correctIndex = 3,
            explanation = "Fluorine has the highest Pauling electronegativity (3.98 / ~4.0) due to its small atomic radius and high effective nuclear charge.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),
        HscMcq(
            id = "mcq_chem_2_1", subjectId = "chemistry", chapterId = "chemistry_2",
            question = "What is the pH of a 0.001 M aqueous HCl solution at 25 degrees Celsius?",
            options = listOf("1", "2", "3", "4"),
            correctIndex = 2,
            explanation = "HCl is a strong monoprotic acid, so [H+] = 10^-3 M, giving pH = -log10(10^-3) = 3.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Sylhet Board '24"
        ),
        HscMcq(
            id = "mcq_chem_2_2", subjectId = "chemistry", chapterId = "chemistry_2",
            question = "Which reagent is used in Tollen's test to distinguish aldehydes from ketones?",
            options = listOf("Ammoniacal silver nitrate", "Alkaline copper sulfate", "Neutral FeCl3", "Bromine water"),
            correctIndex = 0,
            explanation = "Tollen's reagent is ammoniacal silver nitrate [Ag(NH3)2]+, which aldehydes reduce to form a metallic silver mirror.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Rajshahi Board '23"
        ),

        // Higher Math
        HscMcq(
            id = "mcq_math_1_1", subjectId = "higher_math", chapterId = "math_1",
            question = "What is the value of d/dx (ln(sin x))?",
            options = listOf("tan x", "cot x", "sec x", "csc x"),
            correctIndex = 1,
            explanation = "By chain rule, d/dx(ln(sin x)) = (1 / sin x) * cos x = cot x.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_math_1_2", subjectId = "higher_math", chapterId = "math_1",
            question = "A square matrix A is called singular if:",
            options = listOf("|A| = 1", "|A| = 0", "A = A^T", "A^2 = I"),
            correctIndex = 1,
            explanation = "A square matrix with determinant |A| = 0 has no inverse and is called a singular matrix.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),
        HscMcq(
            id = "mcq_math_2_1", subjectId = "higher_math", chapterId = "math_2",
            question = "What is the modulus of the complex number z = 3 - 4i?",
            options = listOf("1", "5", "7", "25"),
            correctIndex = 1,
            explanation = "|z| = sqrt(3^2 + (-4)^2) = sqrt(9 + 16) = sqrt(25) = 5.",
            difficulty = McqDifficulty.EASY, boardTag = "Jashore Board '24"
        ),
        HscMcq(
            id = "mcq_math_2_2", subjectId = "higher_math", chapterId = "math_2",
            question = "For an ellipse, the eccentricity e satisfies which condition?",
            options = listOf("e = 0", "0 < e < 1", "e = 1", "e > 1"),
            correctIndex = 1,
            explanation = "Eccentricity e = 0 is a circle, 0 < e < 1 is an ellipse, e = 1 is a parabola, and e > 1 is a hyperbola.",
            difficulty = McqDifficulty.EASY, boardTag = "NCTB Core"
        ),

        // Biology
        HscMcq(
            id = "mcq_bio_1_1", subjectId = "biology", chapterId = "bio_1",
            question = "During which stage of Meiosis I does crossing-over occur between homologous chromosomes?",
            options = listOf("Leptotene", "Zygotene", "Pachytene", "Diplotene"),
            correctIndex = 2,
            explanation = "Crossing-over and genetic recombination take place at the Pachytene sub-stage of Prophase I.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_bio_1_2", subjectId = "biology", chapterId = "bio_1",
            question = "Which organelle is known as the 'Powerhouse of the Cell' because it produces ATP?",
            options = listOf("Ribosome", "Golgi Body", "Mitochondria", "Lysosome"),
            correctIndex = 2,
            explanation = "Mitochondria carry out aerobic respiration (Krebs cycle and oxidative phosphorylation) to synthesize ATP.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),
        HscMcq(
            id = "mcq_bio_2_1", subjectId = "biology", chapterId = "bio_2",
            question = "Which blood group is known as the 'Universal Donor' for red blood cell transfusion?",
            options = listOf("AB+", "AB-", "O-", "O+"),
            correctIndex = 2,
            explanation = "O-negative red blood cells lack A, B, and Rh antigens, minimizing immune reactions in emergencies.",
            difficulty = McqDifficulty.EASY, boardTag = "Barishal Board '24"
        ),

        // Accounting
        HscMcq(
            id = "mcq_acc_1_1", subjectId = "accounting", chapterId = "acc_1",
            question = "Which fundamental equation forms the basis of double-entry accounting?",
            options = listOf(
                "Assets = Liabilities - Equity",
                "Assets = Liabilities + Owner's Equity",
                "Equity = Assets + Liabilities",
                "Revenue = Expenses + Assets"
            ),
            correctIndex = 1,
            explanation = "The fundamental accounting equation is Assets (A) = Liabilities (L) + Owner's Equity (OE).",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_acc_1_2", subjectId = "accounting", chapterId = "acc_1",
            question = "Which error does a Trial Balance fail to detect?",
            options = listOf(
                "Posting wrong amount on one side",
                "Wrong totaling of a ledger column",
                "Error of complete omission",
                "Recording debit as credit on one account only"
            ),
            correctIndex = 2,
            explanation = "If a transaction is completely omitted from both journal and ledger, both debit and credit totals remain equal, so the Trial Balance still tallies.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Cumilla Board '23"
        ),
        HscMcq(
            id = "mcq_acc_2_1", subjectId = "accounting", chapterId = "acc_2",
            question = "Under the Companies Act 1994 (Bangladesh), what is the minimum number of members for a Public Limited Company?",
            options = listOf("2", "7", "20", "50"),
            correctIndex = 1,
            explanation = "A Public Limited Company in Bangladesh requires at least 7 shareholders (Private Limited requires at least 2).",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '24"
        ),

        // Finance
        HscMcq(
            id = "mcq_fin_1_1", subjectId = "finance", chapterId = "fin_1",
            question = "What does the 'Rule of 72' estimate in Finance?",
            options = listOf(
                "Tax liability of a firm",
                "Years required to double an investment at a given interest rate",
                "Minimum reserve ratio of a bank",
                "Debt-to-equity ratio"
            ),
            correctIndex = 1,
            explanation = "Dividing 72 by the annual compound interest rate approximates the number of years needed to double the principal.",
            difficulty = McqDifficulty.EASY, boardTag = "Rajshahi Board '24"
        ),
        HscMcq(
            id = "mcq_fin_2_1", subjectId = "finance", chapterId = "fin_2",
            question = "Which institution serves as the Central Bank and monetary authority of Bangladesh?",
            options = listOf("Sonali Bank PLC", "Bangladesh Bank", "BSEC", "Agrani Bank PLC"),
            correctIndex = 1,
            explanation = "Bangladesh Bank (established in 1971) is the central bank responsible for currency issuance, monetary policy, and banking regulation.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '23"
        ),

        // Management
        HscMcq(
            id = "mcq_mgmt_1_1", subjectId = "management", chapterId = "mgmt_1",
            question = "Which Partnership Act currently governs partnership businesses in Bangladesh?",
            options = listOf("Partnership Act 1932", "Companies Act 1994", "Contract Act 1872", "Cooperative Act 2001"),
            correctIndex = 0,
            explanation = "Partnership businesses in Bangladesh are governed by the Partnership Act of 1932.",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '23"
        ),
        HscMcq(
            id = "mcq_mgmt_2_1", subjectId = "management", chapterId = "mgmt_2",
            question = "Who is regarded as the Father of Modern Management and author of the 14 Principles of Management?",
            options = listOf("Frederick W. Taylor", "Henri Fayol", "Peter Drucker", "Abraham Maslow"),
            correctIndex = 1,
            explanation = "French management theorist Henri Fayol formulated the 14 Principles of Management and the administrative management theory.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '24"
        ),

        // Economics
        HscMcq(
            id = "mcq_econ_1_1", subjectId = "economics", chapterId = "econ_1",
            question = "According to the Law of Demand (ceteris paribus), when the price of a normal good rises, quantity demanded:",
            options = listOf("Increases", "Decreases", "Remains unchanged", "Becomes infinite"),
            correctIndex = 1,
            explanation = "Price and quantity demanded have an inverse relationship when other factors remain constant.",
            difficulty = McqDifficulty.EASY, boardTag = "Dhaka Board '24"
        ),
        HscMcq(
            id = "mcq_econ_2_1", subjectId = "economics", chapterId = "econ_2",
            question = "Which sector contributes the largest share to Bangladesh's export earnings?",
            options = listOf("Jute & Jute Goods", "Frozen Foods", "Ready-Made Garments (RMG)", "Leather Goods"),
            correctIndex = 2,
            explanation = "The Ready-Made Garments (RMG) sector accounts for over 80% of Bangladesh's total merchandise export earnings.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '24"
        ),

        // Civics
        HscMcq(
            id = "mcq_civ_1_1", subjectId = "civics", chapterId = "civics_1",
            question = "Which of the following is NOT one of the four essential elements of a State?",
            options = listOf("Population", "Territory", "Government", "Political Parties"),
            correctIndex = 3,
            explanation = "The four essential elements of a State are Population, Territory, Government, and Sovereignty.",
            difficulty = McqDifficulty.EASY, boardTag = "Chattogram Board '23"
        ),
        HscMcq(
            id = "mcq_civ_2_1", subjectId = "civics", chapterId = "civics_2",
            question = "In which year was the historic Six-Point Demand (Chhoy Dofa) declared by Bangabandhu?",
            options = listOf("1954", "1966", "1969", "1970"),
            correctIndex = 1,
            explanation = "The Six-Point Demand, known as the Charter of Freedom of the Bengali nation, was announced in February 1966.",
            difficulty = McqDifficulty.EASY, boardTag = "All Boards '24"
        ),

        // Islamic History
        HscMcq(
            id = "mcq_ih_1_1", subjectId = "islamic_history", chapterId = "ih_1",
            question = "Which Abbasid Caliph established the renowned academy 'Bayt al-Hikmah' (House of Wisdom) in Baghdad?",
            options = listOf("Al-Mansur", "Harun al-Rashid", "Al-Ma'mun", "Al-Mutasim"),
            correctIndex = 2,
            explanation = "While Harun al-Rashid laid its early library foundation, Caliph Al-Ma'mun expanded Bayt al-Hikmah into a premier center of translation and science.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Dhaka Board '23"
        ),
        HscMcq(
            id = "mcq_ih_2_1", subjectId = "islamic_history", chapterId = "ih_2",
            question = "Who established the first independent Sultanate in Bengal in 1338 CE?",
            options = listOf("Ikhtiyar Uddin Muhammad Bakhtiyar Khalji", "Fakhruddin Mubarak Shah", "Shamsuddin Ilyas Shah", "Alauddin Husain Shah"),
            correctIndex = 1,
            explanation = "Fakhruddin Mubarak Shah declared independence in Sonargaon in 1338 CE, inaugurating the independent Sultanate period of Bengal.",
            difficulty = McqDifficulty.MEDIUM, boardTag = "Rajshahi Board '24"
        )
    )
}
