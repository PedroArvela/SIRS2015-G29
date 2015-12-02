using System;
using System.Security.Cryptography;
using System.Text;

public class key_gen
{
    static string publicPrivateKeyXML;
    static string publicOnlyKeyXML;

    public static void generate()
	{
        //Generate a public/private key pair.
        RSACryptoServiceProvider RSA = new RSACryptoServiceProvider();
        //Save the public key information to an RSAParameters structure.
        RSAParameters RSAKeyInfo = RSA.ExportParameters(false);

        //Pair of public and private key as XML string.
        publicPrivateKeyXML = RSA.ToXmlString(true);

        //Private key in xml file, this string should be share to other parties
        publicOnlyKeyXML = RSA.ToXmlString(false);

    }

    public static byte[] pbkey()
    {
        return Encoding.Default.GetBytes(publicOnlyKeyXML);
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
}
