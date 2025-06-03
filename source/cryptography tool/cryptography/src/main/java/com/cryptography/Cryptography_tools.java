package com.cryptography;

// Cryptography_tools.java
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays; 
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Cryptography_tools extends JFrame {

    private static final String SETTINGS_FILE = "cryptography_settings.ini";
    private JComboBox<String> algoComboBox;
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private JTextField keyField;
    private JLabel keyLabel;
    private JButton primaryButton; 
    private JButton secondaryButton; 
    private JTextArea descriptionTextArea; // For algorithm descriptions

    // --- Constants for Symmetric Ciphers ---
    // (Same as before)
    private static final String AES_ALGORITHM_FULL = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_SIZE_BYTES = 16; 
    private static final int AES_IV_SIZE_BYTES = 16;

    private static final String AES_GCM_ALGORITHM_FULL = "AES/GCM/NoPadding";
    private static final int AES_GCM_IV_SIZE_BYTES = 12;
    private static final int AES_GCM_TAG_LENGTH_BITS = 128;


    private static final String DES_ALGORITHM_FULL = "DES/CBC/PKCS5Padding";
    private static final int DES_KEY_SIZE_BYTES = 8;
    private static final int DES_IV_SIZE_BYTES = 8;

    private static final String TRIPLE_DES_ALGORITHM_FULL = "DESede/CBC/PKCS5Padding";
    private static final int TRIPLE_DES_KEY_SIZE_BYTES = 24;
    private static final int TRIPLE_DES_IV_SIZE_BYTES = 8;

    private static final String BLOWFISH_ALGORITHM_FULL = "Blowfish/CBC/PKCS5Padding";
    private static final int BLOWFISH_IV_SIZE_BYTES = 8; 

    private static final String RC4_ALGORITHM_FULL = "ARCFOUR";
    private KeyPair rsaKeyPair;


    private final String[] ALGORITHMS = {
            "AES", "AES-GCM", "Affine Cipher", "Atbash Cipher", "Autokey Cipher",
            "Base64", "Beaufort Cipher", "Blowfish", "Caesar Cipher",
            "Columnar Transposition", "DES", "Gronsfeld Cipher", "Hex", "Hill Cipher (2x2)",
            "HMAC-SHA256", "Keyword Cipher", "MD5",
            "One-Time Pad (XOR)", "Playfair Cipher", "RC4 (ARCFOUR)", "ROT13 Cipher",
            "RSA (Demo)", "Rail Fence Cipher", "SHA-1", "SHA-256",
            "Simple Substitution", "TripleDES", "URL Encoding", "Vigenère Cipher"
    };

    public Cryptography_tools() {
        setTitle("Cryptography Tool (Educational)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 850); // Increased size for description area
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- Top Panel: Algorithm Selection and Key Input ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        Arrays.sort(ALGORITHMS); 
        algoComboBox = new JComboBox<>(ALGORITHMS);
        algoComboBox.addActionListener(e -> {
            updateKeyLabelAndButtons();
            updateDescription(); // Update description on selection change
        });

        keyLabel = new JLabel("Parameter:");
        keyField = new JTextField(25); // Slightly reduced from 30

        topPanel.add(new JLabel("Algorithm:"));
        topPanel.add(algoComboBox);
        topPanel.add(keyLabel);
        topPanel.add(keyField);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Description Area ---
        descriptionTextArea = new JTextArea(8, 70); // Rows, Columns
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setBorder(BorderFactory.createTitledBorder("Algorithm Description & Usage"));
        descriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Add some padding around the description panel
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBorder(new EmptyBorder(5,10,5,10));
        descriptionPanel.add(descriptionScrollPane, BorderLayout.CENTER);
        add(descriptionPanel, BorderLayout.CENTER);


        // --- IO and Button Panel (South) ---
        JPanel ioAndButtonPanel = new JPanel(new BorderLayout(10,10));
        
        // Input/Output Text Areas
        inputTextArea = new JTextArea();
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        JScrollPane inputScrollPane = new JScrollPane(inputTextArea);
        inputScrollPane.setBorder(BorderFactory.createTitledBorder("Input Text"));

        outputTextArea = new JTextArea();
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Output Text"));

        JSplitPane textSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScrollPane, outputScrollPane);
        textSplitPane.setResizeWeight(0.5);
        textSplitPane.setBorder(new EmptyBorder(0,10,0,10)); // Keep padding for splitpane itself
        
        ioAndButtonPanel.add(textSplitPane, BorderLayout.CENTER);

        // Button Panel
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        primaryButton = new JButton("Encrypt / Encode");
        secondaryButton = new JButton("Decrypt / Decode");
        JButton loadFileButton = new JButton("Load Text from File");
        JButton saveOutputButton = new JButton("Save Output to File");

        primaryButton.addActionListener(e -> processText(true));
        secondaryButton.addActionListener(e -> processText(false));
        loadFileButton.addActionListener(e -> loadTextFromFile());
        saveOutputButton.addActionListener(e -> saveOutputToFile());

        bottomButtonPanel.add(loadFileButton);
        bottomButtonPanel.add(primaryButton);
        bottomButtonPanel.add(secondaryButton);
        bottomButtonPanel.add(saveOutputButton);
        ioAndButtonPanel.add(bottomButtonPanel, BorderLayout.SOUTH);
        
        add(ioAndButtonPanel, BorderLayout.SOUTH);


        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveSettingsItem = new JMenuItem("Save Settings");
        JMenuItem loadSettingsItem = new JMenuItem("Load Settings");
        JMenuItem exitItem = new JMenuItem("Exit");

        saveSettingsItem.addActionListener(e -> saveSettings());
        loadSettingsItem.addActionListener(e -> loadSettings());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveSettingsItem);
        fileMenu.add(loadSettingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        updateKeyLabelAndButtons();
        updateDescription(); // Initial description
        loadSettings();
    }

    private void updateDescription() {
        String selectedAlgo = (String) algoComboBox.getSelectedItem();
        if (selectedAlgo == null) {
            descriptionTextArea.setText("Select an algorithm to see its description.");
            return;
        }
        String description = "";
        switch (selectedAlgo) {
            case "AES":
                description = "What it does: Advanced Encryption Standard, a strong symmetric block cipher.\n" +
                              "Features: Global standard, supports 128, 192, or 256-bit keys.\n" +
                              "Pros: Very secure with correct usage (strong key, good mode like CBC/GCM, random IV).\n" +
                              "Cons: Key management is crucial. Complex to implement from scratch.\n" +
                              "Usage: Enter a secret key string. App processes it to 16 bytes (AES-128). Output is Base64 encoded (IV prepended for CBC mode).";
                break;
            case "AES-GCM":
                description = "What it does: AES in Galois/Counter Mode. Provides Authenticated Encryption with Associated Data (AEAD).\n" +
                              "Features: Encrypts and authenticates simultaneously, protecting against tampering.\n" +
                              "Pros: High security (confidentiality + integrity). Efficient in hardware.\n" +
                              "Cons: More complex to use than basic CBC. IV uniqueness is absolutely critical for security.\n" +
                              "Usage: Enter a secret key (e.g., 16 chars for AES-128). App processes it to required AES key size. Output is Base64 encoded (IV prepended).";
                break;
            case "Affine Cipher":
                description = "What it does: Monoalphabetic substitution using a linear function (ax + b) mod 26.\n" +
                              "Features: Uses two numeric keys, 'a' and 'b'.\n" +
                              "Pros: Stronger than Caesar, but still very weak against frequency analysis.\n" +
                              "Cons: Easily broken. Key 'a' has restrictions.\n" +
                              "Usage: Key format \"a,b\" (e.g., \"5,8\").\n" +
                              "  - 'a' must be COPRIME with 26. This means 'a' and 26 share no common factors other than 1. Factors of 26 are 1, 2, 13, 26. So, 'a' cannot be even or 13.\n" +
                              "  - Valid 'a' values: 1, 3, 5, 7, 9, 11, 15, 17, 19, 21, 23, 25.\n" +
                              "  - 'b' is the shift, can be any integer (0-25 for direct effect).";
                break;
            case "Atbash Cipher":
                description = "What it does: Reverses the alphabet (A=Z, B=Y, etc.).\n" +
                              "Features: No key needed. Self-reciprocal (applying it twice gives original text).\n" +
                              "Pros: Extremely simple to understand and use.\n" +
                              "Cons: Trivial to break, offers no real security.\n" +
                              "Usage: No key or parameter required.";
                break;
            case "Autokey Cipher":
                description = "What it does: Polyalphabetic cipher where the key is the initial key followed by the plaintext itself (for encryption) or a variant for decryption.\n" +
                              "Features: Key stream depends on the message, making it non-repeating if message is varied.\n" +
                              "Pros: Stronger than simple Vigenère if plaintext is long and non-repetitive, as it avoids short repeating Vigenère keys.\n" +
                              "Cons: If the initial key is short or the plaintext has repetitive patterns, it can be vulnerable. Errors in transmission can propagate. This app's version is simplified.\n" +
                              "Usage: Enter an initial keyword (letters only).";
                break;
            case "Base64":
                description = "What it does: Encodes binary data into a printable ASCII string format. THIS IS NOT ENCRYPTION.\n" +
                              "Features: Uses a 64-character set. Output is approx. 33% larger than input.\n" +
                              "Pros: Useful for transmitting or storing binary data in text-based systems (e.g., email, XML).\n" +
                              "Cons: Provides NO CONFIDENTIALITY. Often mistakenly believed to be encryption.\n" +
                              "Usage: No key or parameter required.";
                break;
            case "Beaufort Cipher":
                description = "What it does: Polyalphabetic substitution similar to Vigenère, using formula C = (K - P) mod 26.\n" +
                              "Features: Self-reciprocal (encrypt and decrypt use the exact same process and key).\n" +
                              "Pros: As strong as the Vigenère cipher.\n" +
                              "Cons: Vulnerable to the same cryptanalysis techniques as Vigenère if the key is short or reused (e.g., Kasiski examination).\n" +
                              "Usage: Enter a keyword (letters only).";
                break;
            case "Blowfish":
                description = "What it does: Symmetric block cipher with a 64-bit block size.\n" +
                              "Features: Variable key length (from 32 bits to 448 bits). Designed to be fast and unpatented.\n" +
                              "Pros: Good performance, generally considered secure. Was a strong candidate before AES.\n" +
                              "Cons: Its 64-bit block size can be a weakness for encrypting very large amounts of data with the same key (potential for Sweet32 attack). AES with 128-bit blocks is generally preferred now.\n" +
                              "Usage: Enter a secret key string. Output is Base64 encoded (IV prepended for CBC mode).";
                break;
            case "Caesar Cipher":
                description = "What it does: Shifts letters of the alphabet by a fixed amount.\n" +
                              "Features: One of the simplest and oldest known encryption techniques.\n" +
                              "Pros: Very easy to understand and implement.\n" +
                              "Cons: Trivial to break by brute force (only 25 possible keys for English alphabet) or frequency analysis.\n" +
                              "Usage: Enter a shift value (an integer, e.g., 3 or -5).";
                break;
            case "Columnar Transposition":
                description = "What it does: Rearranges letters by writing plaintext into a grid under a keyword and reading columns out based on keyword letter order.\n" +
                              "Features: Pure transposition cipher; letter frequencies in ciphertext remain unchanged from plaintext.\n" +
                              "Pros: Simple concept. Can be combined with substitution ciphers for increased (though still classical) strength.\n" +
                              "Cons: Weak on its own. Vulnerable to anagramming and multiple anagramming attacks if enough ciphertext is available. Padding can give clues.\n" +
                              "Usage: Enter a keyword consisting of unique letters (e.g., \"SECRET\"). Plaintext is padded with 'X' if it doesn't fit the grid perfectly.";
                break;
            case "DES":
                description = "What it does: Data Encryption Standard, an older symmetric block cipher with a 64-bit block size.\n" +
                              "Features: Was a widely adopted US government standard for many years.\n" +
                              "Pros: Historically significant and well-understood, including its vulnerabilities.\n" +
                              "Cons: Its 56-bit effective key length is too short for modern security and is vulnerable to brute-force attacks. Superseded by AES.\n" +
                              "Usage: Enter an 8-character secret key (the app uses the first 8 bytes). Output is Base64 encoded (IV prepended for CBC mode).";
                break;
            case "Gronsfeld Cipher":
                description = "What it does: Polyalphabetic substitution, similar to Vigenère but uses digits (0-9) from a numeric key to determine the shift amounts.\n" +
                              "Features: Numeric key for Vigenère-like operation.\n" +
                              "Pros: Simpler key to remember than a Vigenère keyword if numbers are preferred.\n" +
                              "Cons: Weaker than Vigenère if the key is short, as there are only 10 possible shifts per letter. Vulnerable to similar cryptanalysis techniques as Vigenère.\n" +
                              "Usage: Enter a numeric key consisting of digits 0-9 only (e.g., \"1357\").";
                break;
            case "Hex":
                description = "What it does: Encodes binary data into its hexadecimal representation. THIS IS NOT ENCRYPTION.\n" +
                              "Features: Uses characters 0-9 and A-F. Each byte of data becomes two hexadecimal characters.\n" +
                              "Pros: Compact and human-readable representation for byte values.\n" +
                              "Cons: Provides NO CONFIDENTIALITY. \n" +
                              "Usage: No key or parameter required.";
                break;
            case "Hill Cipher (2x2)":
                description = "What it does: Polygraphic substitution cipher using matrix multiplication on pairs of letters (digraphs).\n" +
                              "Features: First polygraphic cipher to use linear algebra. This app implements the 2x2 version.\n" +
                              "Pros: Stronger than simple monoalphabetic substitution as it obscures single letter frequencies by operating on pairs.\n" +
                              "Cons: The key matrix must be invertible modulo 26. Vulnerable to known-plaintext attacks. Larger matrix versions are more secure but more complex.\n" +
                              "Usage: Key format: 4 numbers \"a,b,c,d\" for matrix [[a,b],[c,d]], comma-separated (e.g., \"5,8,17,3\").\n" +
                              "  - The determinant (ad - bc) mod 26 must be COPRIME with 26 (not divisible by 2 or 13). Valid determinants: 1, 3, 5, 7, 9, 11, 15, 17, 19, 21, 23, 25.";
                break;
            case "HMAC-SHA256":
                description = "What it does: Keyed-Hash Message Authentication Code using the SHA-256 hash function. Provides data integrity and authenticity.\n" +
                              "Features: Combines a secret key with the message data and a cryptographic hash function to produce a MAC tag.\n" +
                              "Pros: Strong method to verify that a message has not been tampered with and that it originated from a party possessing the secret key. Widely used standard.\n" +
                              "Cons: Does not provide confidentiality (it's not encryption).\n" +
                              "Usage: Enter a secret key string. Output is the hexadecimal MAC tag. The 'Decrypt / Decode' button is disabled as this is a one-way process.";
                break;
            case "Keyword Cipher":
                description = "What it does: Monoalphabetic substitution where the cipher alphabet is created using a keyword (removing duplicate letters), followed by the remaining unused alphabet letters in their standard order.\n" +
                              "Features: Keyed alphabet generation method.\n" +
                              "Pros: Slightly more complex to guess the key than a completely random simple substitution if the keyword is memorable but not obvious.\n" +
                              "Cons: Still a monoalphabetic substitution, thus vulnerable to frequency analysis given enough ciphertext.\n" +
                              "Usage: Enter a keyword consisting of letters (e.g., \"KRYPTOS\"). Duplicate letters in the keyword are ignored. Case-insensitive.";
                break;
            case "MD5":
                description = "What it does: Message Digest algorithm 5. A widely used hash function producing a 128-bit (16-byte) hash value.\n" +
                              "Features: Historically very common due to its speed.\n" +
                              "Pros: Fast for generating checksums or verifying data integrity in non-security-critical contexts.\n" +
                              "Cons: CRYPTOGRAPHICALLY BROKEN for collision resistance. Practical collision attacks exist. DO NOT USE for security purposes like digital signatures, password hashing, or SSL certificates.\n" +
                              "Usage: No key or parameter. Output is the 32-character hexadecimal hash. 'Decrypt / Decode' button is disabled.";
                break;
            case "One-Time Pad (XOR)":
                description = "What it does: Encrypts by XORing plaintext bytes with key bytes. This app uses a string key, repeating it if shorter than plaintext.\n" +
                              "Features: Theoretically unbreakable IF the key is truly random, used only once, and is at least as long as the message.\n" +
                              "Pros: Perfect secrecy under ideal (and often impractical) conditions. The XOR operation is very simple.\n" +
                              "Cons: THIS APP'S VERSION IS NOT A TRUE OTP AND IS INSECURE for general use because the key is a reusable string, not truly random, and not necessarily single-use. If key is reused, it becomes a simple XOR stream cipher vulnerable to known-plaintext attacks. True OTP key management is extremely difficult.\n" +
                              "Usage: Enter a key string. The same key is used for encryption and decryption (XOR is self-inverse).";
                break;
            case "Playfair Cipher":
                description = "What it does: Digraphic substitution cipher using a 5x5 grid constructed from a keyword. Encrypts pairs of letters (digraphs).\n" +
                              "Features: First practical digraphic substitution cipher. 'J' is usually combined with 'I' in the key table.\n" +
                              "Pros: Stronger than simple monoalphabetic ciphers because it encrypts pairs, which flattens single letter frequency distributions.\n" +
                              "Cons: Still vulnerable to frequency analysis of digraphs if enough ciphertext is available. Relatively easy to break with known techniques. Has specific rules for handling letter pairs (duplicates, odd length).\n" +
                              "Usage: Enter a keyword (letters only, e.g., \"PLAYFAIREXAMPLE\"). 'J' will be treated as 'I'. Plaintext is processed in pairs, padded with 'X' if needed for duplicates or odd length.";
                break;
            case "RC4 (ARCFOUR)":
                description = "What it does: A stream cipher. Its JCA (Java Cryptography Architecture) name is often \"ARCFOUR\".\n" +
                              "Features: Known for its simplicity and speed. Was widely used in protocols like SSL/TLS and WEP.\n" +
                              "Pros: Very fast in software.\n" +
                              "Cons: HAS KNOWN CRYPTOGRAPHIC WEAKNESSES (e.g., biases in the initial bytes of the output keystream, vulnerability to related-key attacks). NOT RECOMMENDED for new applications. AES (especially in a good mode like GCM or CTR) is strongly preferred.\n" +
                              "Usage: Enter a secret key string. Output is Base64 encoded.";
                break;
            case "ROT13 Cipher":
                description = "What it does: A Caesar cipher with a fixed shift of 13 positions.\n" +
                              "Features: Self-reciprocal for letters of the English alphabet (applying ROT13 twice returns the original text).\n" +
                              "Pros: Simple way to obscure text for non-security purposes (e.g., hiding puzzle solutions, spoilers).\n" +
                              "Cons: Offers no real cryptographic security, trivial to reverse.\n" +
                              "Usage: No key or parameter required.";
                break;
            case "RSA (Demo)":
                description = "What it does: Asymmetric (public-key) algorithm for encryption and digital signatures. This is a simplified demo using internally generated keys for encrypting short messages.\n" +
                              "Features: Uses a pair of keys: a public key for encryption and signature verification, and a private key for decryption and signature generation.\n" +
                              "Pros: Allows secure communication without pre-sharing a secret key. Forms the basis of much of modern internet security (TLS/SSL, PGP).\n" +
                              "Cons: Slower than symmetric ciphers like AES. Raw RSA is not typically used for encrypting large amounts of data (hybrid encryption schemes are used instead). Secure key generation, management, and proper padding schemes are critical and complex. This demo is for educational illustration only and not for secure use.\n" +
                              "Usage: Key field is disabled (keys are generated internally for the demo). Input text should be short (e.g., less than 200 bytes) due to RSA block size limits with PKCS#1 v1.5 padding.";
                break;
            case "Rail Fence Cipher":
                description = "What it does: Transposition cipher that writes plaintext characters in a zig-zag pattern across a specified number of \"rails\" and then reads them off row by row.\n" +
                              "Features: Simple geometric transposition method.\n" +
                              "Pros: Easy to understand and implement manually.\n" +
                              "Cons: Very weak and easily broken by visual inspection or simple anagramming techniques, especially with few rails.\n" +
                              "Usage: Enter the number of rails (an integer greater than 1, e.g., 3 or 4).";
                break;
            case "SHA-1":
                description = "What it does: Secure Hash Algorithm 1. Produces a 160-bit (20-byte) hash value.\n" +
                              "Features: Successor to MD5, designed by the NSA.\n" +
                              "Pros: Was widely used for many years in various applications and protocols.\n" +
                              "Cons: CRYPTOGRAPHICALLY WEAKENED. Practical collision attacks have been demonstrated, meaning different inputs can produce the same hash. DEPRECATED for most security uses (e.g., digital signatures, SSL certificates). SHA-256 or SHA-3 should be used instead.\n" +
                              "Usage: No key or parameter. Output is the 40-character hexadecimal hash. 'Decrypt / Decode' button is disabled.";
                break;
            case "SHA-256":
                description = "What it does: Part of the SHA-2 (Secure Hash Algorithm 2) family. Produces a 256-bit (32-byte) hash value.\n" +
                              "Features: Designed by the NSA. Currently a secure and widely adopted standard.\n" +
                              "Pros: Strong collision resistance. Recommended for most new applications requiring a cryptographic hash function.\n" +
                              "Cons: None significant for typical hash function use when used appropriately.\n" +
                              "Usage: No key or parameter. Output is the 64-character hexadecimal hash. 'Decrypt / Decode' button is disabled.";
                break;
            case "Simple Substitution":
                description = "What it does: Each letter in the plaintext is replaced by a corresponding letter from a fixed substitution alphabet (which serves as the key).\n" +
                              "Features: A direct one-to-one mapping for the entire alphabet.\n" +
                              "Pros: Conceptually simple. Stronger than Caesar if the substitution key is random and unknown.\n" +
                              "Cons: Easily broken by frequency analysis if the ciphertext is of sufficient length (a few dozen characters can be enough).\n" +
                              "Usage: Enter a 26-letter key which is a permutation of the standard alphabet (e.g., \"QWERTYUIOPASDFGHJKLZXCVBNM\"). The key is case-insensitive; unique letters are required.";
                break;
            case "TripleDES (3DES)":
                description = "What it does: Applies the Data Encryption Standard (DES) algorithm three times to each data block, typically with two or three different keys.\n" +
                              "Features: Designed to increase the effective key length of DES and address its vulnerability to brute-force attacks.\n" +
                              "Pros: Much stronger than single DES. A well-understood and standardized algorithm.\n" +
                              "Cons: Slow compared to modern ciphers like AES. Its 64-bit block size is a limitation for very large amounts of data (like DES). AES is generally preferred.\n" +
                              "Usage: Enter a secret key (e.g., 24 characters for 3-key 3DES). The app processes this to 24 bytes. Output is Base64 encoded (IV prepended for CBC mode).";
                break;
            case "URL Encoding":
                description = "What it does: Converts characters into a format that can be safely transmitted over the Internet as part of a URL (Uniform Resource Locator). Also known as percent-encoding. THIS IS NOT ENCRYPTION.\n" +
                              "Features: Ensures URLs are valid by encoding special/reserved characters (e.g., a space becomes %20, a '+' becomes %2B).\n" +
                              "Pros: Essential for correct web functionality and data transmission in URLs.\n" +
                              "Cons: Provides NO CONFIDENTIALITY. Its purpose is representation, not security.\n" +
                              "Usage: No key or parameter required.";
                break;
            case "Vigenère Cipher":
                description = "What it does: Polyalphabetic substitution cipher that uses a keyword to apply a series of different Caesar shifts to the letters of the plaintext.\n" +
                              "Features: A significant improvement over monoalphabetic ciphers, known as \"le chiffre indéchiffrable\" (the indecipherable cipher) for a long time.\n" +
                              "Pros: Effectively defeats simple frequency analysis by using multiple cipher alphabets.\n" +
                              "Cons: Vulnerable if the keyword is short or known. Techniques like Kasiski examination and the Friedman test can be used to determine the key length, after which it can be broken as several Caesar ciphers.\n" +
                              "Usage: Enter a keyword consisting of letters (e.g., \"LEMON\").";
                break;
            default:
                description = "Description not available for this algorithm.";
        }
        descriptionTextArea.setText(description);
        descriptionTextArea.setCaretPosition(0); // Scroll to top
    }

    // ... (Rest of the methods: isOneWayAlgorithm, generateRsaKeyPair, updateKeyLabelAndButtons, processText, etc. remain largely the same as the previous full corrected version)
    // Make sure all methods from the previous corrected full version are included here.
    // I will paste the full, integrated code below.
    // --- Helper methods, cipher implementations, file/settings methods from the PREVIOUS full response go here ---
    // (The full code block is too long to repeat here, so I will ensure the main method and constructor changes are correct)

    private boolean isOneWayAlgorithm(String algo) {
        return algo.equals("MD5") || algo.startsWith("SHA-") || algo.startsWith("HMAC-");
    }
    
    private void generateRsaKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048); 
            rsaKeyPair = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this, "RSA algorithm not available: " + e.getMessage(), "RSA Error", JOptionPane.ERROR_MESSAGE);
            rsaKeyPair = null;
        }
    }

    private void updateKeyLabelAndButtons() {
        String selectedAlgo = (String) algoComboBox.getSelectedItem();
        if (selectedAlgo == null) return;

        keyField.setEnabled(true); 
        secondaryButton.setEnabled(true); 
        primaryButton.setText("Encrypt / Encode"); 

        switch (selectedAlgo) {
            case "Caesar Cipher": keyLabel.setText("Shift (number):"); break;
            case "Vigenère Cipher": keyLabel.setText("Keyword (letters):"); break;
            case "AES": keyLabel.setText("Secret Key (e.g., 16 chars):"); break;
            case "AES-GCM": keyLabel.setText("Secret Key (16/24/32 chars):"); break;
            case "DES": keyLabel.setText("Secret Key (e.g., 8 chars):"); break;
            case "TripleDES": keyLabel.setText("Secret Key (e.g., 24 chars):"); break;
            case "Blowfish": keyLabel.setText("Secret Key:"); break;
            case "RC4 (ARCFOUR)": keyLabel.setText("Secret Key:"); break;
            case "Affine Cipher": keyLabel.setText("Keys a,b (e.g., 5,8):"); break;
            case "Simple Substitution": keyLabel.setText("Alphabet Key (26 unique letters):"); break;
            case "Keyword Cipher": keyLabel.setText("Keyword (letters, no repeats):"); break;
            case "Autokey Cipher": keyLabel.setText("Initial Key (letters):"); break;
            case "Beaufort Cipher": keyLabel.setText("Keyword (letters):"); break;
            case "Gronsfeld Cipher": keyLabel.setText("Numeric Key (digits 0-9):"); break;
            case "Rail Fence Cipher": keyLabel.setText("Number of Rails (>1):"); break;
            case "Columnar Transposition": keyLabel.setText("Keyword (letters, no repeats):"); break;
            case "Playfair Cipher": keyLabel.setText("Keyword (letters, J->I):"); break;
            case "Hill Cipher (2x2)": keyLabel.setText("Key: a,b,c,d (4 numbers for 2x2 matrix):"); break;
            case "One-Time Pad (XOR)": keyLabel.setText("Key String (for XOR):"); break;
            case "RSA (Demo)":
                keyLabel.setText("Key: (Generated internally)");
                keyField.setEnabled(false);
                keyField.setText("");
                if (rsaKeyPair == null) generateRsaKeyPair();
                break;
            case "Base64": case "Atbash Cipher": case "ROT13 Cipher": case "Hex": case "URL Encoding":
                keyLabel.setText("Parameter:"); keyField.setEnabled(false); keyField.setText(""); break;
            case "MD5": case "SHA-1": case "SHA-256":
                keyLabel.setText("Parameter:"); keyField.setEnabled(false); keyField.setText("");
                primaryButton.setText("Hash");
                secondaryButton.setEnabled(false);
                break;
            case "HMAC-SHA256":
                keyLabel.setText("Secret Key:");
                primaryButton.setText("Generate MAC");
                secondaryButton.setEnabled(false);
                break;
            default: keyLabel.setText("Parameter:");
        }
    }
    
    private void processText(boolean primaryAction) { 
        String inputText = inputTextArea.getText();
        String keyText = keyField.getText();
        String selectedAlgo = (String) algoComboBox.getSelectedItem();
        String outputText = "";

        if (selectedAlgo == null) {
            JOptionPane.showMessageDialog(this, "Please select an algorithm.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (inputText.isEmpty() && !selectedAlgo.equals("RSA (Demo)")) { 
             JOptionPane.showMessageDialog(this, "Input text cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (isOneWayAlgorithm(selectedAlgo) && !primaryAction) {
             JOptionPane.showMessageDialog(this, selectedAlgo + " is a one-way function and cannot be decrypted/decoded.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            switch (selectedAlgo) {
                case "Caesar Cipher":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Shift value cannot be empty.");
                    int shift = Integer.parseInt(keyText);
                    outputText = caesarCipher(inputText, shift, primaryAction);
                    break;
                case "Vigenère Cipher":
                    if (keyText.isEmpty() || !keyText.matches("[a-zA-Z]+")) throw new IllegalArgumentException("Keyword must be letters and not empty.");
                    outputText = vigenereCipher(inputText, keyText, primaryAction);
                    break;
                case "AES":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key cannot be empty for AES.");
                    SecretKey aesKey = generateSecretKey(keyText, "AES", AES_KEY_SIZE_BYTES);
                    outputText = primaryAction ?
                            symmetricEncryptWithIV(inputText, aesKey, AES_ALGORITHM_FULL, AES_IV_SIZE_BYTES) :
                            symmetricDecryptWithIV(inputText, aesKey, AES_ALGORITHM_FULL, AES_IV_SIZE_BYTES);
                    break;
                case "DES":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key cannot be empty for DES.");
                    SecretKey desKey = generateSecretKey(keyText, "DES", DES_KEY_SIZE_BYTES);
                    outputText = primaryAction ?
                            symmetricEncryptWithIV(inputText, desKey, DES_ALGORITHM_FULL, DES_IV_SIZE_BYTES) :
                            symmetricDecryptWithIV(inputText, desKey, DES_ALGORITHM_FULL, DES_IV_SIZE_BYTES);
                    break;
                case "TripleDES":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key cannot be empty for TripleDES.");
                    SecretKey tripleDesKey = generateSecretKey(keyText, "DESede", TRIPLE_DES_KEY_SIZE_BYTES);
                    outputText = primaryAction ?
                            symmetricEncryptWithIV(inputText, tripleDesKey, TRIPLE_DES_ALGORITHM_FULL, TRIPLE_DES_IV_SIZE_BYTES) :
                            symmetricDecryptWithIV(inputText, tripleDesKey, TRIPLE_DES_ALGORITHM_FULL, TRIPLE_DES_IV_SIZE_BYTES);
                    break;
                case "Base64":
                    outputText = primaryAction ? base64Encode(inputText) : base64Decode(inputText);
                    break;
                case "Atbash Cipher":
                    outputText = atbashCipher(inputText); 
                    break;
                case "ROT13 Cipher":
                    outputText = caesarCipher(inputText, 13, true); 
                    break;
                case "Affine Cipher":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Keys 'a' and 'b' cannot be empty.");
                    String[] parts = keyText.split(",");
                    if (parts.length != 2) throw new IllegalArgumentException("Invalid key format. Use 'a,b'.");
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    outputText = affineCipher(inputText, a, b, primaryAction);
                    break;
                case "Simple Substitution":
                    if (keyText.isEmpty() || keyText.length() != 26 || !isAlphaPermutation(keyText.toUpperCase()))
                        throw new IllegalArgumentException("Key must be a 26-unique-letter permutation.");
                    outputText = simpleSubstitutionCipherForKeyInput(inputText, keyText.toUpperCase(), primaryAction);
                    break;
                case "Hex":
                    outputText = primaryAction ? hexEncode(inputText) : hexDecode(inputText);
                    break;
                case "Keyword Cipher":
                    if (keyText.isEmpty() || !keyText.matches("[a-zA-Z]+")) throw new IllegalArgumentException("Keyword must be letters and not empty.");
                    outputText = keywordCipher(inputText, keyText, primaryAction);
                    break;
                case "Autokey Cipher":
                     if (keyText.isEmpty() || !keyText.matches("[a-zA-Z]+")) throw new IllegalArgumentException("Initial key must be letters and not empty.");
                    outputText = autokeyCipher(inputText, keyText, primaryAction);
                    break;
                case "Beaufort Cipher":
                    if (keyText.isEmpty() || !keyText.matches("[a-zA-Z]+")) throw new IllegalArgumentException("Keyword must be letters and not empty.");
                    outputText = beaufortCipher(inputText, keyText); 
                    break;
                case "Gronsfeld Cipher":
                     if (keyText.isEmpty() || !keyText.matches("[0-9]+")) throw new IllegalArgumentException("Numeric key must be digits 0-9 and not empty.");
                    outputText = gronsfeldCipher(inputText, keyText, primaryAction);
                    break;
                case "Rail Fence Cipher":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Number of rails cannot be empty.");
                    int rails = Integer.parseInt(keyText);
                    if (rails <= 1) throw new IllegalArgumentException("Number of rails must be greater than 1.");
                    outputText = primaryAction ? railFenceEncrypt(inputText, rails) : railFenceDecrypt(inputText, rails);
                    break;
                case "Columnar Transposition":
                    if (keyText.isEmpty() || !isAlphaPermutationUnique(keyText)) throw new IllegalArgumentException("Keyword must be unique letters and not empty.");
                    outputText = primaryAction ? columnarTranspositionEncrypt(inputText, keyText) : columnarTranspositionDecrypt(inputText, keyText);
                    break;
                case "Playfair Cipher":
                     if (keyText.isEmpty() || !keyText.matches("[a-zA-Z]+")) throw new IllegalArgumentException("Keyword must be letters and not empty.");
                    outputText = playfairCipher(inputText, keyText, primaryAction);
                    break;
                case "Hill Cipher (2x2)":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Hill Cipher key cannot be empty.");
                    outputText = hillCipher2x2(inputText, keyText, primaryAction);
                    break;
                case "One-Time Pad (XOR)":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Key for XOR cannot be empty.");
                    outputText = otpXorCipher(inputText, keyText); 
                    break;
                case "Blowfish":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key for Blowfish cannot be empty.");
                    SecretKey blowfishKey = generateSecretKey(keyText, "Blowfish", -1); 
                    outputText = primaryAction ?
                            symmetricEncryptWithIV(inputText, blowfishKey, BLOWFISH_ALGORITHM_FULL, BLOWFISH_IV_SIZE_BYTES) :
                            symmetricDecryptWithIV(inputText, blowfishKey, BLOWFISH_ALGORITHM_FULL, BLOWFISH_IV_SIZE_BYTES);
                    break;
                case "RC4 (ARCFOUR)":
                     if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key for RC4 cannot be empty.");
                    SecretKey rc4Key = generateSecretKey(keyText, "ARCFOUR", -1); 
                    outputText = primaryAction ?
                            symmetricEncryptNoIV(inputText, rc4Key, RC4_ALGORITHM_FULL) :
                            symmetricDecryptNoIV(inputText, rc4Key, RC4_ALGORITHM_FULL);
                    break;
                case "AES-GCM":
                    if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key for AES-GCM cannot be empty.");
                     SecretKey aesGcmKey = generateSecretKey(keyText, "AES", AES_KEY_SIZE_BYTES);
                    outputText = primaryAction ?
                            aesGcmEncrypt(inputText, aesGcmKey) :
                            aesGcmDecrypt(inputText, aesGcmKey);
                    break;
                case "URL Encoding":
                    outputText = primaryAction ? urlEncode(inputText) : urlDecode(inputText);
                    break;
                case "MD5":
                    outputText = hashText(inputText, "MD5");
                    break;
                case "SHA-1":
                    outputText = hashText(inputText, "SHA-1");
                    break;
                case "SHA-256":
                    outputText = hashText(inputText, "SHA-256");
                    break;
                case "HMAC-SHA256":
                     if (keyText.isEmpty()) throw new IllegalArgumentException("Secret Key for HMAC cannot be empty.");
                    outputText = generateMac(inputText, keyText, "HmacSHA256");
                    break;
                case "RSA (Demo)":
                    if (rsaKeyPair == null) {
                        generateRsaKeyPair();
                        if (rsaKeyPair == null) throw new RuntimeException("RSA key pair could not be generated.");
                    }
                     outputText = primaryAction ? rsaEncrypt(inputText, rsaKeyPair.getPublic()) : rsaDecrypt(inputText, rsaKeyPair.getPrivate());
                    break;

                default:
                    JOptionPane.showMessageDialog(this, "Algorithm '" + selectedAlgo + "' not fully implemented yet.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
            }
            outputTextArea.setText(outputText);
        } catch (NumberFormatException e) {
            outputTextArea.setText("Error: Invalid number format in key field. " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Invalid number in key/parameter: " + e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (IllegalArgumentException e) {
            outputTextArea.setText("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Input Error: " + e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            outputTextArea.setText("Operation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            // e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Operation failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private SecretKey generateSecretKey(String keyString, String algorithm, int fixedKeySizeInBytes) throws Exception {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        if (fixedKeySizeInBytes > 0) { 
            byte[] fixedKey = new byte[fixedKeySizeInBytes];
            System.arraycopy(keyBytes, 0, fixedKey, 0, Math.min(keyBytes.length, fixedKey.length));
            if (algorithm.equals("DES")) {
                KeySpec spec = new DESKeySpec(fixedKey);
                return SecretKeyFactory.getInstance("DES").generateSecret(spec);
            } else if (algorithm.equals("DESede")) {
                KeySpec spec = new DESedeKeySpec(fixedKey);
                return SecretKeyFactory.getInstance("DESede").generateSecret(spec);
            }
            return new SecretKeySpec(fixedKey, algorithm);
        } else { 
            if (algorithm.equals("Blowfish") || algorithm.equals("ARCFOUR")) {
                return new SecretKeySpec(keyBytes, algorithm);
            }
            throw new NoSuchAlgorithmException("Unsupported algorithm for flexible key generation: " + algorithm);
        }
    }

    private String symmetricEncryptWithIV(String plainText, SecretKey secretKey, String transformation, int ivSizeBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        byte[] iv = new byte[ivSizeBytes];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] ivAndEncryptedBytes = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndEncryptedBytes, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndEncryptedBytes, iv.length, encryptedBytes.length);
        return Base64.getEncoder().encodeToString(ivAndEncryptedBytes);
    }

    private String symmetricDecryptWithIV(String base64EncryptedText, SecretKey secretKey, String transformation, int ivSizeBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        byte[] ivAndEncryptedBytes = Base64.getDecoder().decode(base64EncryptedText);

        if (ivAndEncryptedBytes.length < ivSizeBytes) {
            throw new IllegalArgumentException("Encrypted text is too short to contain IV.");
        }
        byte[] iv = Arrays.copyOfRange(ivAndEncryptedBytes, 0, ivSizeBytes);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        byte[] encryptedBytes = Arrays.copyOfRange(ivAndEncryptedBytes, ivSizeBytes, ivAndEncryptedBytes.length);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    
    private String symmetricEncryptNoIV(String plainText, SecretKey secretKey, String transformation) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String symmetricDecryptNoIV(String base64EncryptedText, SecretKey secretKey, String transformation) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] encryptedBytes = Base64.getDecoder().decode(base64EncryptedText);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private String aesGcmEncrypt(String plainText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM_FULL);
        byte[] iv = new byte[AES_GCM_IV_SIZE_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_LENGTH_BITS, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        byte[] ivAndEncryptedBytes = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndEncryptedBytes, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndEncryptedBytes, iv.length, encryptedBytes.length);
        return Base64.getEncoder().encodeToString(ivAndEncryptedBytes);
    }

    private String aesGcmDecrypt(String base64EncryptedText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM_FULL);
        byte[] ivAndEncryptedBytes = Base64.getDecoder().decode(base64EncryptedText);

        if (ivAndEncryptedBytes.length < AES_GCM_IV_SIZE_BYTES) {
            throw new IllegalArgumentException("Encrypted GCM text is too short to contain IV.");
        }
        byte[] iv = Arrays.copyOfRange(ivAndEncryptedBytes, 0, AES_GCM_IV_SIZE_BYTES);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_LENGTH_BITS, iv);
        byte[] encryptedBytes = Arrays.copyOfRange(ivAndEncryptedBytes, AES_GCM_IV_SIZE_BYTES, ivAndEncryptedBytes.length);
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private String rsaEncrypt(String plainText, PublicKey publicKey) throws Exception {
        if (plainText.getBytes(StandardCharsets.UTF_8).length > (2048/8 - 11)) { 
            throw new IllegalArgumentException("Text too long for single RSA block encryption demo (max ~245 bytes).");
        }
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String rsaDecrypt(String base64EncryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encryptedBytes = Base64.getDecoder().decode(base64EncryptedText);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private String hashText(String text, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashedBytes);
    }

    private String generateMac(String text, String keyString, String algorithm) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(secretKeySpec);
        byte[] macBytes = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(macBytes);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String caesarCipher(String text, int shift, boolean encrypt) {
        StringBuilder result = new StringBuilder();
        int effShift = encrypt ? shift : -shift;
        for (char character : text.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isLowerCase(character) ? 'a' : 'A';
                result.append((char) (base + (character - base + effShift % 26 + 26) % 26));
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private String vigenereCipher(String text, String key, boolean encrypt) {
        StringBuilder result = new StringBuilder();
        String upperKey = key.toUpperCase().replaceAll("[^A-Z]", "");
        if (upperKey.isEmpty()) throw new IllegalArgumentException("Vigenere key must contain letters.");
        int keyIndex = 0;
        for (char character : text.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isLowerCase(character) ? 'a' : 'A';
                int shiftAmount = upperKey.charAt(keyIndex % upperKey.length()) - 'A';
                if (!encrypt) shiftAmount = -shiftAmount;
                result.append((char) (base + (character - base + shiftAmount + 26) % 26));
                keyIndex++;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

     private String atbashCipher(String text) {
        StringBuilder result = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (Character.isLowerCase(character)) result.append((char) ('z' - (character - 'a')));
            else if (Character.isUpperCase(character)) result.append((char) ('Z' - (character - 'A')));
            else result.append(character);
        }
        return result.toString();
    }

    private int modInverse(int a, int m) {
        a = a % m;
        for (int x = 1; x < m; x++) if ((a * x) % m == 1) return x;
        return -1;
    }

    private String affineCipher(String text, int a, int b, boolean encrypt) {
        if (modInverse(a, 26) == -1) throw new IllegalArgumentException("'a' ("+a+") must be coprime to 26.");
        StringBuilder result = new StringBuilder();
        if (encrypt) {
            for (char ch : text.toCharArray()) {
                if (Character.isLetter(ch)) {
                    char base = Character.isLowerCase(ch) ? 'a' : 'A';
                    result.append((char) (base + (a * (ch - base) + b) % 26));
                } else result.append(ch);
            }
        } else {
            int a_inv = modInverse(a, 26);
            for (char ch : text.toCharArray()) {
                if (Character.isLetter(ch)) {
                    char base = Character.isLowerCase(ch) ? 'a' : 'A';
                    result.append((char) (base + (a_inv * (ch - base - b % 26 + 26)) % 26));
                } else result.append(ch);
            }
        }
        return result.toString();
    }

    private boolean isAlphaPermutation(String key) { 
        if (key.length() != 26) return false;
        Set<Character> chars = new HashSet<>();
        for (char c : key.toCharArray()) { 
            if (!Character.isLetter(c) || !chars.add(c)) return false;
        }
        return chars.size() == 26;
    }

     private boolean isAlphaPermutationUnique(String key) { 
        if (key.isEmpty()) return false;
        Set<Character> chars = new HashSet<>();
        for (char c : key.toUpperCase().toCharArray()) {
            if (!Character.isLetter(c) || !chars.add(c)) return false;
        }
        return true; 
    }

    private String simpleSubstitutionCipher(String text, String sourceAlphabet, String targetAlphabet) {
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            boolean isLower = Character.isLowerCase(ch);
            char upperCh = Character.toUpperCase(ch);
            int index = sourceAlphabet.indexOf(upperCh);
            if (index != -1) { 
                char substitutedChar = targetAlphabet.charAt(index);
                result.append(isLower ? Character.toLowerCase(substitutedChar) : substitutedChar);
            } else {
                result.append(ch); 
            }
        }
        return result.toString();
    }

    private String simpleSubstitutionCipherForKeyInput(String text, String fullTargetAlphabetKey, boolean encrypt) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (encrypt) {
            return simpleSubstitutionCipher(text, alphabet, fullTargetAlphabetKey); 
        } else { 
            return simpleSubstitutionCipher(text, fullTargetAlphabetKey, alphabet); 
        }
    }

    private String base64Encode(String text) { return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)); }
    private String base64Decode(String text) { return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8); }

    private String hexEncode(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
    private String hexDecode(String hexText) {
        if (hexText.length() % 2 != 0) throw new IllegalArgumentException("Hex string must have an even number of chars.");
        byte[] bytes = new byte[hexText.length() / 2];
        try {
            for (int i = 0; i < hexText.length(); i += 2) {
                bytes[i / 2] = (byte) Integer.parseInt(hexText.substring(i, i + 2), 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid character in hex string.", e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String keywordCipher(String text, String keyword, boolean encrypt) {
        String upperKeyword = keyword.toUpperCase().replaceAll("[^A-Z]", "");
        String distinctKeyword = upperKeyword.chars().distinct()
                                    .mapToObj(c -> String.valueOf((char)c))
                                    .collect(Collectors.joining());
        if (distinctKeyword.isEmpty()) throw new IllegalArgumentException("Keyword must contain letters.");

        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder cipherAlphabetBuilder = new StringBuilder(distinctKeyword);
        for (char c : alphabet.toCharArray()) {
            if (distinctKeyword.indexOf(c) == -1) { 
                cipherAlphabetBuilder.append(c);
            }
        }
        String cipherAlphabet = cipherAlphabetBuilder.toString();

        if (encrypt) {
            return simpleSubstitutionCipher(text, alphabet, cipherAlphabet);
        } else {
            return simpleSubstitutionCipher(text, cipherAlphabet, alphabet);
        }
    }

    private String autokeyCipher(String text, String initialKey, boolean encrypt) {
        String initialKeyUpper = initialKey.toUpperCase().replaceAll("[^A-Z]", "");
        if (initialKeyUpper.isEmpty()) throw new IllegalArgumentException("Initial key for Autokey must contain letters.");
        String textToProcess = text.toUpperCase().replaceAll("[^A-Z]", ""); 

        StringBuilder result = new StringBuilder();
        int letterIndex = 0; 

        for (int i = 0; i < text.length(); i++) {
            char originalChar = text.charAt(i);
            if (Character.isLetter(originalChar)) {
                char base = Character.isLowerCase(originalChar) ? 'a' : 'A';
                int charValue = Character.toUpperCase(originalChar) - 'A'; 

                char keyStreamChar;
                if (letterIndex < initialKeyUpper.length()) {
                    keyStreamChar = initialKeyUpper.charAt(letterIndex);
                } else { 
                    if (encrypt) {
                         if ((letterIndex - initialKeyUpper.length()) >= textToProcess.length()){
                            // This case might happen if original text had many non-letters
                            // For simplicity, we'll just repeat the initial key if textToProcess is exhausted.
                            // A more robust solution would be more complex.
                            keyStreamChar = initialKeyUpper.charAt((letterIndex - initialKeyUpper.length()) % initialKeyUpper.length());
                        } else {
                            keyStreamChar = textToProcess.charAt(letterIndex - initialKeyUpper.length());
                        }
                    } else { // Decrypt
                         if ((letterIndex - initialKeyUpper.length()) >= textToProcess.length()){
                             keyStreamChar = initialKeyUpper.charAt((letterIndex - initialKeyUpper.length()) % initialKeyUpper.length());
                         } else {
                            keyStreamChar = result.toString().toUpperCase().replaceAll("[^A-Z]", "").charAt(letterIndex - initialKeyUpper.length());
                         }
                    }
                }
                int keyValue = keyStreamChar - 'A';
                int processedValue;

                if (encrypt) {
                    processedValue = (charValue + keyValue) % 26;
                } else { 
                    processedValue = (charValue - keyValue + 26) % 26;
                }
                result.append((char) (base + processedValue));
                letterIndex++;
            } else {
                result.append(originalChar); 
            }
        }
        return result.toString();
    }


    private String beaufortCipher(String text, String keyword) { 
        StringBuilder result = new StringBuilder();
        String upperKeyword = keyword.toUpperCase().replaceAll("[^A-Z]","");
        if (upperKeyword.isEmpty()) throw new IllegalArgumentException("Beaufort key must contain letters.");
        int keyIndex = 0;
        for (char character : text.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isLowerCase(character) ? 'a' : 'A';
                int textVal = Character.toUpperCase(character) - 'A';
                int keyVal = upperKeyword.charAt(keyIndex % upperKeyword.length()) - 'A';
                result.append((char) (base + (keyVal - textVal + 26) % 26));
                keyIndex++;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private String gronsfeldCipher(String text, String numericKey, boolean encrypt) {
        StringBuilder result = new StringBuilder();
        if (numericKey.isEmpty() || !numericKey.matches("[0-9]+")) throw new IllegalArgumentException("Gronsfeld key must be digits and not empty.");
        int keyIndex = 0;
        for (char character : text.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isLowerCase(character) ? 'a' : 'A';
                int shift = numericKey.charAt(keyIndex % numericKey.length()) - '0';
                if (!encrypt) shift = -shift;
                result.append((char) (base + (character - base + shift + 26) % 26));
                keyIndex++;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private String railFenceEncrypt(String text, int rails) {
        if (rails <= 1) return text;
        char[][] fence = new char[rails][text.length()];
        // No need to fill with '\0' if we only append non-null chars later

        int row = 0;
        boolean down = true;
        for (int col = 0; col < text.length(); col++) { // Iterate by characters of text
            fence[row][col] = text.charAt(col);
            if (row == 0) down = true;
            else if (row == rails - 1) down = false;
            row += down ? 1 : -1;
        }

        StringBuilder result = new StringBuilder();
        for (int r = 0; r < rails; r++) {
            for (int c = 0; c < text.length(); c++) { 
                 if (fence[r][c] != '\0') result.append(fence[r][c]); // Check for null char if matrix not perfectly filled
            }
        }
        return result.toString();
    }

    private String railFenceDecrypt(String cipher, int rails) {
        if (rails <= 1) return cipher;
        char[][] fence = new char[rails][cipher.length()];
        // Mark positions with placeholder
        int row = 0;
        boolean down = true;
        for (int col = 0; col < cipher.length(); col++) { // Iterate by length of cipher
            fence[row][col] = '*'; 
            if (row == 0) down = true;
            else if (row == rails - 1) down = false;
            row += down ? 1 : -1;
        }

        int index = 0;
        for (int r = 0; r < rails; r++) {
            for (int c = 0; c < cipher.length(); c++) {
                if (fence[r][c] == '*' && index < cipher.length()) {
                    fence[r][c] = cipher.charAt(index++);
                }
            }
        }

        StringBuilder result = new StringBuilder();
        row = 0; down = true;
        for (int c = 0; c < cipher.length(); c++) { // Read out column by column, following the path
            result.append(fence[row][c]);
            if (row == 0) down = true;
            else if (row == rails - 1) down = false;
            row += down ? 1 : -1;
        }
        return result.toString();
    }
    
    private static class PlayfairPair { int r, c; PlayfairPair(int r, int c) { this.r = r; this.c = c; }}

    private char[][] generatePlayfairKeyTable(String keyword) {
        String upperKeyword = keyword.toUpperCase().replaceAll("[^A-Z]", "").replace("J", "I");
        String distinctKey = upperKeyword.chars().distinct().mapToObj(c -> String.valueOf((char)c)).collect(Collectors.joining());
        if (distinctKey.isEmpty() && !keyword.isEmpty() && keyword.matches("[a-zA-Z]+")) { // Handle J only keyword
             distinctKey = "I";
        } else if (distinctKey.isEmpty() && keyword.isEmpty()){
             throw new IllegalArgumentException("Playfair keyword cannot be empty after processing.");
        }


        StringBuilder alphabetBuilder = new StringBuilder(distinctKey);
        for (char c = 'A'; c <= 'Z'; c++) {
            if (c == 'J') continue;
            if (distinctKey.indexOf(c) == -1) alphabetBuilder.append(c);
        }
        if (alphabetBuilder.length() > 25) alphabetBuilder.setLength(25); // Ensure it's not > 25
        
        char[][] table = new char[5][5];
        for (int i = 0; i < 25; i++) {
            if (i < alphabetBuilder.length())
                table[i/5][i%5] = alphabetBuilder.charAt(i);
            else // Should not happen if logic is correct
                table[i/5][i%5] = 'X'; // Fallback, though this indicates an issue
        }
        return table;
    }

    private PlayfairPair findCharInPlayfairTable(char[][] table, char ch) {
        if (ch == 'J') ch = 'I'; 
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) if (table[i][j] == ch) return new PlayfairPair(i, j);
        return null; 
    }
    
    private String playfairCipher(String text, String keyword, boolean encrypt) {
        char[][] table = generatePlayfairKeyTable(keyword);
        String processedText = text.toUpperCase().replaceAll("[^A-Z]", "").replace("J", "I");
        
        StringBuilder digraphText = new StringBuilder();
        for (int i = 0; i < processedText.length(); i++) {
            digraphText.append(processedText.charAt(i));
            if (i + 1 < processedText.length()) {
                if (processedText.charAt(i) == processedText.charAt(i+1)) {
                    digraphText.append('X'); 
                }
            }
        }
        if (digraphText.length() % 2 != 0) digraphText.append('X'); 

        StringBuilder result = new StringBuilder();
        int dir = encrypt ? 1 : -1; 

        for (int i = 0; i < digraphText.length(); i += 2) {
            char c1 = digraphText.charAt(i);
            char c2 = digraphText.charAt(i+1);
            PlayfairPair p1 = findCharInPlayfairTable(table, c1);
            PlayfairPair p2 = findCharInPlayfairTable(table, c2);

            if (p1 == null || p2 == null) throw new IllegalArgumentException("Invalid char in Playfair input after processing: " + c1 + " or " + c2 + ". Check keyword and input.");


            if (p1.r == p2.r) { 
                result.append(table[p1.r][(p1.c + dir + 5) % 5]);
                result.append(table[p2.r][(p2.c + dir + 5) % 5]);
            } else if (p1.c == p2.c) { 
                result.append(table[(p1.r + dir + 5) % 5][p1.c]);
                result.append(table[(p2.r + dir + 5) % 5][p2.c]);
            } else { 
                result.append(table[p1.r][p2.c]);
                result.append(table[p2.r][p1.c]);
            }
        }
        return result.toString(); 
    }


    private String columnarTranspositionEncrypt(String text, String key) {
        String upperKey = key.toUpperCase().replaceAll("[^A-Z]", "");
        String distinctKey = upperKey.chars().distinct().mapToObj(c -> String.valueOf((char)c)).collect(Collectors.joining());
        if (distinctKey.isEmpty()) throw new IllegalArgumentException("Columnar key must contain letters.");

        int numCols = distinctKey.length();
        int numRows = (int) Math.ceil((double) text.length() / numCols);
        char[][] matrix = new char[numRows][numCols];
        
        int k = 0;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (k < text.length()) matrix[i][j] = text.charAt(k++);
                else matrix[i][j] = 'X'; 
            }
        }
        
        char[] sortedKeyChars = distinctKey.toCharArray();
        Arrays.sort(sortedKeyChars);
        
        Integer[] colOrder = new Integer[numCols];
        List<Character> distinctKeyList = new ArrayList<>();
        for(char c : distinctKey.toCharArray()) distinctKeyList.add(c);

        int currentOrderIdx = 0;
        for(char sortedChar : sortedKeyChars){
            for(int i=0; i<distinctKeyList.size(); i++){
                if(distinctKeyList.get(i) == sortedChar){
                    colOrder[currentOrderIdx++] = i;
                    distinctKeyList.set(i, (char)0); // Mark as used for non-unique original keys (though distinctKey should be unique)
                    break;
                }
            }
        }
        
        StringBuilder ciphertext = new StringBuilder();
        for (int colIdxToRead : colOrder) {
            for (int i = 0; i < numRows; i++) {
                ciphertext.append(matrix[i][colIdxToRead]);
            }
        }
        return ciphertext.toString();
    }

    private String columnarTranspositionDecrypt(String cipher, String key) {
        String upperKey = key.toUpperCase().replaceAll("[^A-Z]", "");
        String distinctKey = upperKey.chars().distinct().mapToObj(c -> String.valueOf((char)c)).collect(Collectors.joining());
        if (distinctKey.isEmpty()) throw new IllegalArgumentException("Columnar key must contain letters.");

        int numCols = distinctKey.length();
        if (cipher.length() % numCols != 0) {
            throw new IllegalArgumentException("Ciphertext length ("+cipher.length()+") must be a multiple of key length ("+numCols+") for this simplified decryption.");
        }
        int numRows = cipher.length() / numCols; 


        char[][] matrix = new char[numRows][numCols];

        char[] sortedKeyChars = distinctKey.toCharArray();
        Arrays.sort(sortedKeyChars);
        
        Integer[] colOrder = new Integer[numCols];
        List<Character> distinctKeyList = new ArrayList<>();
        for(char c : distinctKey.toCharArray()) distinctKeyList.add(c);
        
        int currentOrderIdx = 0;
        for(char sortedChar : sortedKeyChars){
             for(int i=0; i<distinctKeyList.size(); i++){
                if(distinctKeyList.get(i) == sortedChar){
                    colOrder[currentOrderIdx++] = i;
                    distinctKeyList.set(i, (char)0); 
                    break;
                }
            }
        }
        
        int k = 0;
        for (int originalColIdxToFill : colOrder) { 
            for (int i = 0; i < numRows; i++) {
                if (k < cipher.length()) matrix[i][originalColIdxToFill] = cipher.charAt(k++);
            }
        }
        
        StringBuilder plaintext = new StringBuilder();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                plaintext.append(matrix[i][j]);
            }
        }
        return plaintext.toString();
    }


    private String hillCipher2x2(String text, String keyStr, boolean encrypt) {
        String[] keyParts = keyStr.split(",");
        if (keyParts.length != 4) throw new IllegalArgumentException("Hill key needs 4 numbers a,b,c,d (comma-separated).");
        int[][] keyMatrix = new int[2][2];
        try {
            keyMatrix[0][0] = Integer.parseInt(keyParts[0].trim()); keyMatrix[0][1] = Integer.parseInt(keyParts[1].trim());
            keyMatrix[1][0] = Integer.parseInt(keyParts[2].trim()); keyMatrix[1][1] = Integer.parseInt(keyParts[3].trim());
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Hill key parts must be integer numbers."); }

        int det = (keyMatrix[0][0] * keyMatrix[1][1] - keyMatrix[0][1] * keyMatrix[1][0]);
        det = (det % 26 + 26) % 26; 
        
        int detInv = modInverse(det, 26);
        if (detInv == -1) throw new IllegalArgumentException("Key matrix is not invertible (determinant " + det + " has no mod 26 inverse). Valid determinants are 1,3,5,7,9,11,15,17,19,21,23,25.");

        int[][] matrixToUse;
        if (encrypt) {
            matrixToUse = keyMatrix;
        } else { 
            matrixToUse = new int[2][2];
            matrixToUse[0][0] = (keyMatrix[1][1] * detInv % 26 + 26) % 26;
            matrixToUse[0][1] = (-keyMatrix[0][1] * detInv % 26 + 26) % 26;
            matrixToUse[1][0] = (-keyMatrix[1][0] * detInv % 26 + 26) % 26;
            matrixToUse[1][1] = (keyMatrix[0][0] * detInv % 26 + 26) % 26;
        }

        String preparedText = text.toUpperCase().replaceAll("[^A-Z]", "");
        if (preparedText.length() % 2 != 0) preparedText += "X"; 

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < preparedText.length(); i += 2) {
            int p1 = preparedText.charAt(i) - 'A';
            int p2 = preparedText.charAt(i+1) - 'A';
            result.append((char) ('A' + (matrixToUse[0][0]*p1 + matrixToUse[0][1]*p2) % 26));
            result.append((char) ('A' + (matrixToUse[1][0]*p1 + matrixToUse[1][1]*p2) % 26));
        }
        return result.toString();
    }

    private String otpXorCipher(String text, String key) { 
        StringBuilder result = new StringBuilder();
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8); 
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 0) throw new IllegalArgumentException("OTP Key cannot be empty.");

        for (int i = 0; i < textBytes.length; i++) {
            result.append((char) (textBytes[i] ^ keyBytes[i % keyBytes.length]));
        }
        return result.toString();
    }
    
    private String urlEncode(String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
    }
    private String urlDecode(String text) throws UnsupportedEncodingException {
        return URLDecoder.decode(text, StandardCharsets.UTF_8.toString());
    }

    private void loadTextFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a .txt file to load");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(fileChooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
                inputTextArea.setText(content);
                outputTextArea.setText("");
            } catch (IOException e) { JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void saveOutputToFile() {
        if (outputTextArea.getText().isEmpty()) { JOptionPane.showMessageDialog(this, "Output is empty.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Output As");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) file = new File(file.getParentFile(), file.getName() + ".txt");
            try {
                Files.writeString(file.toPath(), outputTextArea.getText(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Output saved to " + file.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) { JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("algorithm", (String) algoComboBox.getSelectedItem());
        if (keyField.isEnabled()) props.setProperty("keyParameter", keyField.getText());
        else props.setProperty("keyParameter", "");
         if (rsaKeyPair != null && "RSA (Demo)".equals(algoComboBox.getSelectedItem())) {
            props.setProperty("rsaPublicKey", Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded()));
            props.setProperty("rsaPrivateKey", Base64.getEncoder().encodeToString(rsaKeyPair.getPrivate().getEncoded()));
        }

        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            props.store(output, "Cryptography App Settings");
        } catch (IOException io) { JOptionPane.showMessageDialog(this, "Error saving settings: " + io.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void loadSettings() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);
        if (!settingsFile.exists()) {
            algoComboBox.setSelectedItem("Caesar Cipher"); keyField.setText("3");
            updateKeyLabelAndButtons(); updateDescription(); return;
        }
        try (InputStream input = new FileInputStream(settingsFile)) {
            props.load(input);
            String algo = props.getProperty("algorithm");
            String keyParam = props.getProperty("keyParameter");

            if (algo != null && Arrays.asList(ALGORITHMS).contains(algo)) algoComboBox.setSelectedItem(algo);
            else algoComboBox.setSelectedItem("Caesar Cipher");
            
            updateKeyLabelAndButtons(); 
            updateDescription(); // Call after algo is set

            if (keyField.isEnabled() && keyParam != null) keyField.setText(keyParam);
            else if (keyField.isEnabled()) keyField.setText(""); 
            else keyField.setText(""); 

             if ("RSA (Demo)".equals(algoComboBox.getSelectedItem())) { // Check current selection
                String pubKeyStr = props.getProperty("rsaPublicKey");
                String privKeyStr = props.getProperty("rsaPrivateKey");
                if (pubKeyStr != null && privKeyStr != null && !pubKeyStr.isEmpty() && !privKeyStr.isEmpty()) {
                    try {
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pubKeyStr)));
                        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privKeyStr)));
                        rsaKeyPair = new KeyPair(pub, priv);
                    } catch (Exception e) {
                        rsaKeyPair = null; 
                         generateRsaKeyPair(); 
                    }
                } else { // If keys not in settings or empty, ensure rsaKeyPair is null or freshly generated
                    if (rsaKeyPair != null) rsaKeyPair = null; // Clear if it was set by previous selection
                    generateRsaKeyPair(); // And generate fresh ones
                }
            }
        } catch (IOException ex) {
            algoComboBox.setSelectedItem("Caesar Cipher"); keyField.setText("3"); 
            updateKeyLabelAndButtons(); 
            updateDescription();
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) { System.err.println("Could not set system L&F: " + e.getMessage()); }
        SwingUtilities.invokeLater(() -> new Cryptography_tools().setVisible(true));
    }
}