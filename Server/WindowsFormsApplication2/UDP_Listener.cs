﻿using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

public class UDPListener
{
    private const int listenPort = 8888;

    private static void StartListener()
    {
        bool done = false;

        UdpClient listener = new UdpClient(listenPort);
        IPEndPoint groupEP = new IPEndPoint(IPAddress.Any, listenPort);

        try
        {
            while (!done)
            {
                Console.WriteLine("Waiting for broadcast");
                byte[] bytes = listener.Receive(ref groupEP);

                Console.WriteLine("Received broadcast from {0} :\n {1}\n",
                    groupEP.ToString(),
                    Encoding.ASCII.GetString(bytes, 0, bytes.Length));
                done = true;
            }

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
        finally
        {
            listener.Close();
        }
    }

    public static int run()
    {
        StartListener();

        return 0;
    }
}