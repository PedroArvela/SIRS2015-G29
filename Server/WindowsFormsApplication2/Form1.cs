using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;


namespace WindowsFormsApplication2
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();


        }

        RSACryptoServiceProvider rsa = null;
        string publicPrivateKeyXML;
        string publicOnlyKeyXML;
    
        public void AssignNewKey()
        {
            const int PROVIDER_RSA_FULL = 1;
            const string CONTAINER_NAME = "KeyContainer";
            CspParameters cspParams;
            cspParams = new CspParameters(PROVIDER_RSA_FULL);
            cspParams.KeyContainerName = CONTAINER_NAME;
            cspParams.Flags = CspProviderFlags.UseMachineKeyStore;
            cspParams.ProviderName = "Microsoft Strong Cryptographic Provider";
            rsa = new RSACryptoServiceProvider(cspParams);

            //Pair of public and private key as XML string.
            publicPrivateKeyXML = rsa.ToXmlString(true);

            //Private key in xml file, this string should be share to other parties
            publicOnlyKeyXML = rsa.ToXmlString(false);
        }

        public byte[] Encrypt(string publicKeyXML, string dataToDycript)
        {
            RSACryptoServiceProvider rsa = new RSACryptoServiceProvider();
            rsa.FromXmlString(publicKeyXML);

            return rsa.Encrypt(ASCIIEncoding.ASCII.GetBytes(dataToDycript), true);
        }

        public string Decrypt(string publicPrivateKeyXML, byte[] encryptedData)
        {
            RSACryptoServiceProvider rsa = new RSACryptoServiceProvider();
            rsa.FromXmlString(publicPrivateKeyXML);

            return ASCIIEncoding.ASCII.GetString(rsa.Decrypt(encryptedData, true));
        }

        private void button1_Click(object sender, EventArgs e)
        {
            AssignNewKey();
            UDP_Sender.sender(publicOnlyKeyXML);
            label1.Text = "Broadcasted Public Key!";

        }



        private void label1_Click(object sender, EventArgs e)
        {
        }

        private void Form1_Load(object sender, EventArgs e)
        {

        }

        private void button2_Click(object sender, EventArgs e)
        {
            AssignNewKey();
            label1.Text = "Generation Successfull!";
        }

        private void button3_Click(object sender, EventArgs e)
        {
            UDPListener.run();
            label1.Text = "Message Received";
        }
    }
}
