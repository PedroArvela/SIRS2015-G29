using System;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using WindowsFormsApplication2;

class UDP_Sender
{

    internal static void sender(String pbkey)
    {
        Socket s = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
            ProtocolType.Udp);

        IPAddress broadcast = IPAddress.Parse("192.168.1.255");

        byte[] sendbuf = Encoding.ASCII.GetBytes(pbkey);
        IPEndPoint ep = new IPEndPoint(broadcast, 8082);

        s.SendTo(sendbuf, ep);

    }

}
