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
    public partial class SIRS : Form
    {
        public SIRS()
        {
            InitializeComponent();


        }

        string publicOnlyKeyXML = "asjdasdlsjdalksjdasldjlksjkjda";


        private void button1_Click(object sender, EventArgs e)
        {
            //AssignNewKey();
            //UDP_Sender.sender();
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
            key_gen.generate();
            string p = key_gen.pbkey().Length.ToString();
            //AssignNewKey();
            label1.Text = p;
        }

        private void button3_Click(object sender, EventArgs e)
        {
            UDPListener.run();
            label1.Text = "Message Received";
        }

        private void button4_Click(object sender, EventArgs e)
        {
            key_gen.generate();
            string pb = System.Text.Encoding.UTF8.GetString(key_gen.pbkey());
            UDP_Sender.sendinfo(publicOnlyKeyXML);
            label1.Text = "Info sent sucessfully!";
            //UDP_Sender.sender(publicOnlyKeyXML);
            label1.Text = "Pbkey sent sucessfully!";


        }
    }
}
