package com.c256.foenix;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;

public class SerialTool
{
	SerialPort serial = null;

	public byte TxLRC = 0;
	public byte RxLRC = 0;
	public byte[] Stat0 = new byte[1];
	public byte[] Stat1 = new byte[1];
	public byte[] LRC = new byte[1];
	public String[] ports;

	public static class CommandLineArgs
	{
		public boolean isPortList    = false;
		public String fileName       = null;
		public String startAddress   = null;
		public String dataLength     = null;
		public String outputfileName = null;
		public String comPortName 	 = null;
		public boolean isArgError	 = false;
		public boolean dump			 = false;
		public boolean help			 = false;	
		
		
		public CommandLineArgs(String[] args)
		{
			int len = 0;
			
			try
			{
				do
				{
					String curArg = args[len];
					
					if("-l".equals(curArg))
					{
						isPortList = true;
					}
					else if("--help".equals(curArg))
					{
						help = true;
					}
					else if("--dump".equals(curArg))
					{
						dump = true;
					}
					else if("-f".equals(curArg))
					{
						len++;
						fileName = args[len];					
					}
					else if("-o".equals(curArg))
					{
						len++;
						outputfileName = args[len];					
					}
					else if("-a".equals(curArg))
					{
						len++;
						startAddress = args[len];					
					}
					else if("-s".equals(curArg))
					{
						len++;
						dataLength = args[len];					
					}
					else if("-p".equals(curArg))
					{
						len++;
						comPortName  = args[len];					
					} 
					else if("-d".equals(curArg))
					{
						len++;
						comPortName  = args[len];					
					} 
					
					len++;
				}
				while(len < args.length);
			}
			catch (Exception e)
			{
				isArgError = true;
			}
		}
		
	}
	
	
	public static void main(String[] args)
	{
		boolean bRead = true;

		
		CommandLineArgs arguments = new CommandLineArgs(args);
		
		
		
		SerialPort[] ports = SerialPort.getCommPorts();
		if (ports != null && ports.length > 0)
		{
			SerialPort fxdebug = null;

			if(arguments.isPortList)
			{
				for(SerialPort port : ports)
				{
					System.out.println("Port:" + port.getSystemPortName());

					System.out.println("\tDesc  :" + port.getDescriptivePortName() + "[" + port.getPortDescription() + "]");

					System.out.println("\tBaud  :" + port.getBaudRate());
					System.out.println("\tData  :" + port.getNumDataBits());
					System.out.println("\tStop  :" + port.getNumStopBits());
					System.out.println("\tParity:" + port.getParity());
					System.out.println("\tHS:" + port.getFlowControlSettings());
				}

				return;
			}
			
			if(arguments.comPortName!=null)
			{
				for(SerialPort port : ports)
				{
					//if("XR21B1411".equals(port.getPortDescription()))
					if(arguments.comPortName.equals(port.getPortDescription()) ||  arguments.comPortName.equals(port.getSystemPortName()))
					{
						fxdebug = port;
					}
				}
			}			
			
			/*
			for (SerialPort port : ports)
			{
				System.out.println("Port:" + port.getSystemPortName());

				System.out.println("\tDesc  :" + port.getDescriptivePortName() + "[" + port.getPortDescription() + "]");

				System.out.println("\tBaud  :" + port.getBaudRate());
				System.out.println("\tData  :" + port.getNumDataBits());
				System.out.println("\tStop  :" + port.getNumStopBits());
				System.out.println("\tParity:" + port.getParity());
				System.out.println("\tHS	:" + port.getFlowControlSettings());

				if ("XR21B1411".equals(port.getPortDescription()))
				{
					fxdebug = port;
				}
			}
			*/
			
			if(fxdebug!=null && arguments.startAddress!=null && arguments.dataLength!=null)
			{
				SerialTool st = new SerialTool(fxdebug);
				if (st != null)
				{
					if (bRead)
					{
						int radix = 10;
						
						if(arguments.startAddress.toLowerCase().startsWith("0x"))
							radix = 16;
						
						int nStartAddress = Integer.parseInt(arguments.startAddress.replace("0x", ""), radix);

						if(arguments.dataLength.toLowerCase().startsWith("0x"))
							radix = 16;
						
						int nDatalength   = Integer.parseInt(arguments.dataLength.replace("0x", ""), radix);
						
						byte[] buffer = null;

						if(arguments.outputfileName!=null)
						{
							buffer = new byte[nDatalength];
							
							boolean read = st.fetchData(buffer, nStartAddress, nDatalength);
							if (read)
							{
								try
								{
									st.exportData(arguments.outputfileName,buffer);
								}
								catch (IOException e)
								{
									e.printStackTrace();
								}

								if(arguments.dump)
									dumpBuffer(buffer);
							}
						}
						else if(arguments.fileName!=null)
						{
							try
							{
								buffer = st.importData(arguments.fileName);
								if(buffer!=null)
								{
									st.sendData(buffer, nStartAddress, buffer.length);

									if(arguments.dump)
										dumpBuffer(buffer);
								}
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	private static void dumpBuffer(byte[] buffer)
	{
		int perLine = 16;
		
		for(byte b : buffer)
		{
			String byteOut = Integer.toHexString(Byte.toUnsignedInt(b));
			if (byteOut.length() < 2)
				System.out.print("0");

			System.out.print(byteOut.toUpperCase());
			System.out.print(" ");

			if(perLine == 9)
				System.out.print(" ");
			
			perLine--;

			if (perLine < 0)
			{
				perLine = 16;
				System.out.println();
			}
		}
	}

	public SerialTool(SerialPort selectedPort)
	{
		this.serial = selectedPort;

		SerialSettings ss = new SerialSettings();

		serial.setBaudRate(ss.getBaudRate());
		serial.setNumDataBits(ss.getDataBits());
		serial.setNumStopBits(ss.getStopBits());
		serial.setFlowControl(ss.getHandshake().getSetting());
		serial.setParity(ss.getParity());

		serial.setComPortTimeouts(2000, 2000, 2000);
		serial.setParity(ss.getParity());

		/*
		System.out.println("Foenix Debug:" + serial.getSystemPortName());
		System.out.println("\tBaud  :" + serial.getBaudRate());
		System.out.println("\tData  :" + serial.getNumDataBits());
		System.out.println("\tStop  :" + serial.getNumStopBits());
		System.out.println("\tParity:" + serial.getParity());
		System.out.println("\tHS	:" + serial.getFlowControlSettings());
		*/
	}

	public void GetFnxInDebugMode()
	{
		byte[] commandBuffer = new byte[8];
		commandBuffer[0] = 0x55; // Header
		commandBuffer[1] = (byte) 0x80; // GetFNXinDebugMode
		commandBuffer[2] = 0x00;
		commandBuffer[3] = 0x00;
		commandBuffer[4] = 0x00;
		commandBuffer[5] = 0x00;
		commandBuffer[6] = 0x00;
		commandBuffer[7] = (byte) 0xD5;
		SendMessage(commandBuffer, null);
	}

	public void ExitFnxDebugMode()
	{
		byte[] commandBuffer = new byte[8];
		commandBuffer[0] = 0x55; // Header
		commandBuffer[1] = (byte) 0x81; // ExitFNXinDebugMode
		commandBuffer[2] = 0x00;
		commandBuffer[3] = 0x00;
		commandBuffer[4] = 0x00;
		commandBuffer[5] = 0x00;
		commandBuffer[6] = 0x00;
		commandBuffer[7] = (byte) 0xD4;
		SendMessage(commandBuffer, null);
	}

	public void sendData(byte[] buffer, int startAddress, int size)
	{
		try
		{
			boolean openedPort = serial.openPort();
			
			if (serial.isOpen())
			{
				// Get into Debug mode (Reset the CPU and keep it in that state
				// and Gavin will take control of the bus)
				GetFnxInDebugMode();
				// Now's let's transfer the code
				if (size <= 2048)
				{
					// DataBuffer = The buffer where the loaded Binary File
					// resides
					// FnxAddressPtr = Pointer where to put the Data in the Fnx
					// i = Pointer Inside the data buffer
					// Size_Of_File = Size of the Payload we want to transfer
					// which ought to be smaller than 8192
					PreparePacket2Write(buffer, startAddress, 0, size);
					// UploadProgressBar.Increment(size);
				}
				else
				{
					int BufferSize = 2048;
					int Loop = size / BufferSize;
					int offset = startAddress;
					for (int j = 0; j < Loop; j++)
					{
						PreparePacket2Write(buffer, offset, j * BufferSize, BufferSize);
						offset = offset + BufferSize; // Advance the Pointer to
														// the next location
														// where to write Data
														// in the Foenix
						// UploadProgressBar.Increment(BufferSize);
					}
					BufferSize = (size % BufferSize);
					if (BufferSize > 0)
					{
						PreparePacket2Write(buffer, offset, size - BufferSize, BufferSize);
						// UploadProgressBar.Increment(BufferSize);
					}
				}

				// Update the Reset Vectors from the Binary Files Considering
				// that the Files Keeps the Vector @ $00:FF00
				if (startAddress < 0xFF00 && (startAddress + buffer.length) > 0xFFFF
						|| (startAddress == 0x18_0000 && buffer.length > 0xFFFF))
				{
					PreparePacket2Write(buffer, 0x00FF00, 0x00FF00, 256);
				}

				// The Loading of the File is Done, Reset the FNX and Get out of
				// Debug Mode
				ExitFnxDebugMode();

				System.out.println("Transfer Done! System Reset!\n" + "Send Binary Success");
			}

		}
		catch (Exception ex)
		{
			ExitFnxDebugMode();
			System.out.println(ex.getMessage() + "Send Binary Error");
		}
	}

	public boolean fetchData(byte[] buffer, int startAddress, int size)
	{
		boolean success = false;
		try
		{
			boolean openedPort = serial.openPort();

			//System.out.println("PORT STATUS:" + openedPort);

			if (serial.isOpen())
			{
				try
				{
					GetFnxInDebugMode();
					if (size < 2048)
					{
						PreparePacket2Read(buffer, startAddress, 0, size);
						// UploadProgressBar.Increment(size);
					}
					else
					{
						int BufferSize = 2048;
						int Loop = size / BufferSize;

						for (int j = 0; j < Loop; j++)
						{
							PreparePacket2Read(buffer, startAddress, j * BufferSize, BufferSize);
							startAddress += BufferSize; // Advance the Pointer
														// to the next location
														// where to write Data
														// in the Foenix
							// UploadProgressBar.Increment(BufferSize);
						}
						BufferSize = (size % BufferSize);
						if (BufferSize > 0)
						{
							PreparePacket2Read(buffer, startAddress, size - BufferSize, BufferSize);
							// UploadProgressBar.Increment(BufferSize);
						}
					}

					ExitFnxDebugMode();
					success = true;
				}
				catch (Exception e)
				{
					serial.closePort();
					e.printStackTrace();
				}
			}
		}
		catch (Exception ex)
		{
			ExitFnxDebugMode();
			System.out.println(ex.getMessage() + "Fetch Data Error");
		}
		return success;
	}

	public void PreparePacket2Write(byte[] buffer, int FNXMemPointer, int FilePointer, int Size)
	{
		// Maximum transmission size is 8192
		if (Size > 8192)
			Size = 8192;

		byte[] commandBuffer = new byte[8 + Size];
		commandBuffer[0] = 0x55; // Header
		commandBuffer[1] = 0x01; // Write 2 Memory
		commandBuffer[2] = (byte) ((FNXMemPointer >> 16) & 0xFF); // (H)24Bit
																	// Addy -
																	// Where to
																	// Store the
																	// Data
		commandBuffer[3] = (byte) ((FNXMemPointer >> 8) & 0xFF); // (M)24Bit
																	// Addy -
																	// Where to
																	// Store the
																	// Data
		commandBuffer[4] = (byte) (FNXMemPointer & 0xFF); // (L)24Bit Addy -
															// Where to Store
															// the Data
		commandBuffer[5] = (byte) ((Size >> 8) & 0xFF); // (H)16Bit Size - How
														// many bytes to Store
														// (Max 8Kbytes 4 Now)
		commandBuffer[6] = (byte) (Size & 0xFF); // (L)16Bit Size - How many
													// bytes to Store (Max
													// 8Kbytes 4 Now)
		System.arraycopy(buffer, FilePointer, commandBuffer, 7, Size);

		TxProcessLRC(commandBuffer);
		//System.out.println("Transmit Data LRC:" + TxLRC);
		// commandBuffer[Size + 7] = TxLRC;

		SendMessage(commandBuffer, null); // Tx the requested Payload Size (Plus
											// Header and LRC), No Payload to be
											// received aside of the Status.
	}

	public void PreparePacket2Read(byte[] receiveBuffer, int address, int offset, int size)
	{
		if (size > 0)
		{
			byte[] commandBuffer = new byte[8];
			commandBuffer[0] = 0x55; // Header
			commandBuffer[1] = 0x00; // Command READ Memory
			commandBuffer[2] = (byte) (address >> 16); // Address Hi
			commandBuffer[3] = (byte) (address >> 8); // Address Med
			commandBuffer[4] = (byte) (address & 0xFF); // Address Lo
			commandBuffer[5] = (byte) (size >> 8); // Size HI
			commandBuffer[6] = (byte) (size & 0xFF); // Size LO
			commandBuffer[7] = 0x5A;

			byte[] partialBuffer = new byte[size];
			SendMessage(commandBuffer, partialBuffer);
			System.arraycopy(partialBuffer, 0, receiveBuffer, offset, size);
		}
	}

	public void SendMessage(byte[] command, byte[] data)
	{
		// int dwStartTime = System.Environment.TickCount;
		int i;
		byte[] byte_buffer = new byte[1];

		int wroteBytes = serial.writeBytes(command, command.length, 0);
		//System.out.println("wroteBytes:" + wroteBytes);

		Stat0[0] = 0;
		Stat1[0] = 0;
		LRC[0] = 0;

		int readBytes = 0;

		do
		{
			byte_buffer[0] = (byte) 0;
			readBytes = serial.readBytes(byte_buffer, 1);
			/*
			if (byte_buffer[0] != 0)
			{
				System.out.println("VALUE:" + Integer.toHexString(byte_buffer[0] & 0x000000FF));
			}
			*/
		}
		while (byte_buffer[0] != (byte) 0xAA);

		//System.out.println("readBytes:" + readBytes);

		if (byte_buffer[0] == (byte) 0xAA)
		{
			byte[] bb = new byte[1];

			serial.readBytes(Stat0, 1);
			serial.readBytes(Stat1, 1);
			if (data != null)
			{
				for (i = 0; i < data.length; i++)
				{
					serial.readBytes(bb, 1);
					data[i] = bb[0];
				}
			}
			serial.readBytes(LRC, 1);
		}

		RxLRC = (byte) RxProcessLRC(data);
		//System.out.println("Receive Data LRC:" + RxLRC);
	}

	public int TxProcessLRC(byte[] buffer)
	{
		int i;
		TxLRC = 0;
		for (i = 0; i < buffer.length; i++)
			TxLRC = (byte) (TxLRC ^ buffer[i]);
		return TxLRC;
	}

	public int RxProcessLRC(byte[] data)
	{
		int i;
		RxLRC = (byte) 0xAA;
		RxLRC = (byte) (RxLRC ^ Stat0[0]);
		RxLRC = (byte) (RxLRC ^ Stat1[0]);
		if (data != null)
		{
			for (i = 0; i < data.length; i++)
				RxLRC = (byte) (RxLRC ^ data[i]);
		}
		RxLRC = (byte) (RxLRC ^ LRC[0]);
		return RxLRC;
	}

	public void exportData(String filename, byte[] data) throws IOException
	{
		ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filename));
		if(outputStream!=null)
		{
			try
			{
				try
				{
					outputStream.writeObject(data);
				}
				catch(Exception e)
				{
					
				}
			}
			finally
			{
				if(outputStream!=null)
					outputStream.close();
			}
		}
	}	

	public byte[] importData(String filename) throws IOException
	{
		byte[] buffer = null;
		
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filename));
		if(inputStream!=null)
		{
			try
			{
				try
				{
					buffer = (byte[]) inputStream.readObject();
				}
				catch(Exception e)
				{
					
				}
			}
			finally
			{
				if(inputStream!=null)
					inputStream.close();
			}
		}
		
		return buffer;
	}	

}
