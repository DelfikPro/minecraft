package net.minecraft.world.chunk.anvil;

import net.minecraft.logging.Log;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.AnvilConverterException;
import net.minecraft.world.biome.IChunkBiomer;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraft.world.storage.SaveFormatOld;
import net.minecraft.world.storage.WorldInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AnvilSaveConverter extends SaveFormatOld {

	private static final Log logger = Log.MAIN;

	public AnvilSaveConverter(File p_i2144_1_) {
		super(p_i2144_1_);
	}

	/**
	 * Returns the name of the save format.
	 */
	public String getName() {
		return "Anvil";
	}

	public List<SaveFormatComparator> getSaveList() throws AnvilConverterException {
		if (this.savesDirectory != null && this.savesDirectory.exists() && this.savesDirectory.isDirectory()) {
			List<SaveFormatComparator> list = new ArrayList<>();
			File[] afile = this.savesDirectory.listFiles();

			for (File file1 : afile) {
				if (file1.isDirectory()) {
					String s = file1.getName();
					WorldInfo worldinfo = this.getWorldInfo(s);

					if (worldinfo != null && (worldinfo.getSaveVersion() == 19132 || worldinfo.getSaveVersion() == 19133)) {
						boolean flag = worldinfo.getSaveVersion() != this.getSaveVersion();
						String s1 = worldinfo.getWorldName();

						if (StringUtils.isEmpty(s1)) s1 = s;

						long i = 0L;
						list.add(new SaveFormatComparator(s, s1, worldinfo.getLastTimePlayed(), i, worldinfo.getGameType(), flag, worldinfo.isHardcoreModeEnabled(), worldinfo.areCommandsAllowed()));
					}
				}
			}

			return list;
		}
		throw new AnvilConverterException("Unable to read or access folder where game worlds are saved!");
	}

	protected int getSaveVersion() {
		return 19133;
	}

	public void flushCache() {
		RegionFileCache.clearRegionFileReferences();
	}

	/**
	 * Returns back a loader for the specified save directory
	 */
	public ISaveHandler getSaveLoader(String saveName, boolean storePlayerdata) {
		return new AnvilSaveHandler(this.savesDirectory, saveName, storePlayerdata);
	}

	public boolean func_154334_a(String saveName) {
		WorldInfo worldinfo = this.getWorldInfo(saveName);
		return worldinfo != null && worldinfo.getSaveVersion() == 19132;
	}

	/**
	 * gets if the map is old chunk saving (true) or McRegion (false)
	 */
	public boolean isOldMapFormat(String saveName) {
		WorldInfo worldinfo = this.getWorldInfo(saveName);
		return worldinfo != null && worldinfo.getSaveVersion() != this.getSaveVersion();
	}

	/**
	 * converts the map to mcRegion
	 */
	public boolean convertMapFormat(String filename, IProgressUpdate progressCallback) {
		throw new IllegalStateException("Конвертация старых карт в новые пока не поддерживается.");
		//		progressCallback.setLoadingProgress(0);
		//		List<File> list = new java.util.ArrayList<>();
		//		List<File> list1 = new java.util.ArrayList<>();
		//		List<File> list2 = new java.util.ArrayList<>();
		//		File file1 = new File(this.savesDirectory, filename);
		//		File file2 = new File(file1, "DIM-1");
		//		File file3 = new File(file1, "DIM1");
		//		logger.info("Scanning folders...");
		//		this.addRegionFilesToCollection(file1, list);
		//
		//		if (file2.exists()) {
		//			this.addRegionFilesToCollection(file2, list1);
		//		}
		//
		//		if (file3.exists()) {
		//			this.addRegionFilesToCollection(file3, list2);
		//		}
		//
		//		int i = list.size() + list1.size() + list2.size();
		//		logger.info("Total conversion count is " + i);
		//		WorldInfo worldinfo = this.getWorldInfo(filename);
		//		IChunkManager worldchunkmanager = worldinfo.getTerrainType().createChunkManager(worldinfo.getSeed(), worldinfo.getGeneratorOptions());
		//
		//		this.convertFile(new File(file1, "region"), list, worldchunkmanager, 0, i, progressCallback);
		//		this.convertFile(new File(file2, "region"), list1, new WorldChunkManagerHell(BiomeGenBase.hell, 0.0F), list.size(), i, progressCallback);
		//		this.convertFile(new File(file3, "region"), list2, new WorldChunkManagerHell(BiomeGenBase.sky, 0.0F), list.size() + list1.size(), i, progressCallback);
		//		worldinfo.setSaveVersion(19133);
		//
		//		if (worldinfo.getTerrainType() == WorldType.DEFAULT_1_1) {
		//			worldinfo.setTerrainType(WorldType.DEFAULT);
		//		}
		//
		//		this.createFile(filename);
		//		ISaveHandler isavehandler = this.getSaveLoader(filename, false);
		//		isavehandler.saveWorldInfo(worldinfo);
		//		return true;
	}

	/**
	 * par: filename for the level.dat_mcr backup
	 */
	private void createFile(String filename) {
		File file1 = new File(this.savesDirectory, filename);

		if (!file1.exists()) {
			logger.warn("Unable to create level.dat_mcr backup");
		} else {
			File file2 = new File(file1, "level.dat");

			if (!file2.exists()) {
				logger.warn("Unable to create level.dat_mcr backup");
			} else {
				File file3 = new File(file1, "level.dat_mcr");

				if (!file2.renameTo(file3)) {
					logger.warn("Unable to create level.dat_mcr backup");
				}
			}
		}
	}

	private void convertFile(File p_75813_1_, Iterable<File> p_75813_2_, IChunkBiomer p_75813_3_, int p_75813_4_, int p_75813_5_, IProgressUpdate p_75813_6_) {
		for (File file1 : p_75813_2_) {
			this.convertChunks(p_75813_1_, file1, p_75813_3_, p_75813_4_, p_75813_5_, p_75813_6_);
			++p_75813_4_;
			int i = (int) Math.round(100.0D * (double) p_75813_4_ / (double) p_75813_5_);
			p_75813_6_.setLoadingProgress(i);
		}
	}

	/**
	 * copies a 32x32 chunk set from par2File to par1File, via AnvilConverterData
	 */
	private void convertChunks(File p_75811_1_, File p_75811_2_, IChunkBiomer p_75811_3_, int p_75811_4_, int p_75811_5_, IProgressUpdate progressCallback) {
		try {
			String s = p_75811_2_.getName();
			RegionFile regionfile = new RegionFile(p_75811_2_);
			RegionFile regionfile1 = new RegionFile(new File(p_75811_1_, s.substring(0, s.length() - ".mcr".length()) + ".mca"));

			for (int i = 0; i < 32; ++i) {
				for (int j = 0; j < 32; ++j) {
					if (regionfile.isChunkSaved(i, j) && !regionfile1.isChunkSaved(i, j)) {
						DataInputStream datainputstream = regionfile.getChunkDataInputStream(i, j);

						if (datainputstream == null) {
							logger.warn("Failed to fetch input stream");
						} else {
							NBTTagCompound nbttagcompound = CompressedStreamTools.read(datainputstream);
							datainputstream.close();
							NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("Level");
							AnvilConverterData chunkloader$anvilconverterdata = load(nbttagcompound1);
							NBTTagCompound nbttagcompound2 = new NBTTagCompound();
							NBTTagCompound nbttagcompound3 = new NBTTagCompound();
							nbttagcompound2.setTag("Level", nbttagcompound3);
							convertToAnvilFormat(chunkloader$anvilconverterdata, nbttagcompound3, p_75811_3_);
							DataOutputStream dataoutputstream = regionfile1.getChunkDataOutputStream(i, j);
							CompressedStreamTools.write(nbttagcompound2, dataoutputstream);
							dataoutputstream.close();
						}
					}
				}

				int k = (int) Math.round(100.0D * (double) (p_75811_4_ * 1024) / (double) (p_75811_5_ * 1024));
				int l = (int) Math.round(100.0D * (double) ((i + 1) * 32 + p_75811_4_ * 1024) / (double) (p_75811_5_ * 1024));

				if (l > k) {
					progressCallback.setLoadingProgress(l);
				}
			}

			regionfile.close();
			regionfile1.close();
		} catch (IOException ioexception) {
			ioexception.printStackTrace();
		}
	}

	/**
	 * filters the files in the par1 directory, and adds them to the par2 collections
	 */
	private void addRegionFilesToCollection(File worldDir, Collection<File> collection) {
		File file1 = new File(worldDir, "region");
		File[] afile = file1.listFiles((file, s) -> s.endsWith(".mcr"));

		if (afile != null) {
			Collections.addAll(collection, afile);
		}
	}

	private static AnvilConverterData load(NBTTagCompound nbt) {
		int i = nbt.getInteger("xPos");
		int j = nbt.getInteger("zPos");
		AnvilConverterData chunkloader$anvilconverterdata = new AnvilConverterData(i, j);
		chunkloader$anvilconverterdata.blocks = nbt.getByteArray("Blocks");
		chunkloader$anvilconverterdata.data = new NibbleArrayReader(nbt.getByteArray("Data"), 7);
		chunkloader$anvilconverterdata.skyLight = new NibbleArrayReader(nbt.getByteArray("SkyLight"), 7);
		chunkloader$anvilconverterdata.blockLight = new NibbleArrayReader(nbt.getByteArray("BlockLight"), 7);
		chunkloader$anvilconverterdata.heightmap = nbt.getByteArray("HeightMap");
		chunkloader$anvilconverterdata.terrainPopulated = nbt.getBoolean("TerrainPopulated");
		chunkloader$anvilconverterdata.entities = nbt.getTagList("Entities", 10);
		chunkloader$anvilconverterdata.tileEntities = nbt.getTagList("TileEntities", 10);
		chunkloader$anvilconverterdata.tileTicks = nbt.getTagList("TileTicks", 10);

		try {
			chunkloader$anvilconverterdata.lastUpdated = nbt.getLong("LastUpdate");
		} catch (ClassCastException var5) {
			chunkloader$anvilconverterdata.lastUpdated = (long) nbt.getInteger("LastUpdate");
		}

		return chunkloader$anvilconverterdata;
	}

	private static void convertToAnvilFormat(AnvilConverterData p_76690_0_, NBTTagCompound nbt, IChunkBiomer biomer) {
		nbt.setInteger("xPos", p_76690_0_.x);
		nbt.setInteger("zPos", p_76690_0_.z);
		nbt.setLong("LastUpdate", p_76690_0_.lastUpdated);
		int[] aint = new int[p_76690_0_.heightmap.length];

		for (int i = 0; i < p_76690_0_.heightmap.length; ++i) {
			aint[i] = p_76690_0_.heightmap[i];
		}

		nbt.setIntArray("HeightMap", aint);
		nbt.setBoolean("TerrainPopulated", p_76690_0_.terrainPopulated);
		NBTTagList nbttaglist = new NBTTagList();

		for (int j = 0; j < 8; ++j) {
			boolean flag = true;

			for (int k = 0; k < 16 && flag; ++k) {
				for (int l = 0; l < 16 && flag; ++l) {
					for (int i1 = 0; i1 < 16; ++i1) {
						int j1 = k << 11 | i1 << 7 | l + (j << 4);
						int k1 = p_76690_0_.blocks[j1];

						if (k1 != 0) {
							flag = false;
							break;
						}
					}
				}
			}

			if (!flag) {
				byte[] abyte1 = new byte[4096];
				NibbleArray nibblearray = new NibbleArray();
				NibbleArray nibblearray1 = new NibbleArray();
				NibbleArray nibblearray2 = new NibbleArray();

				for (int j3 = 0; j3 < 16; ++j3) {
					for (int l1 = 0; l1 < 16; ++l1) {
						for (int i2 = 0; i2 < 16; ++i2) {
							int j2 = j3 << 11 | i2 << 7 | l1 + (j << 4);
							int k2 = p_76690_0_.blocks[j2];
							abyte1[l1 << 8 | i2 << 4 | j3] = (byte) (k2 & 255);
							nibblearray.set(j3, l1, i2, p_76690_0_.data.get(j3, l1 + (j << 4), i2));
							nibblearray1.set(j3, l1, i2, p_76690_0_.skyLight.get(j3, l1 + (j << 4), i2));
							nibblearray2.set(j3, l1, i2, p_76690_0_.blockLight.get(j3, l1 + (j << 4), i2));
						}
					}
				}

				NBTTagCompound nbttagcompound = new NBTTagCompound();
				nbttagcompound.setByte("Y", (byte) (j & 255));
				nbttagcompound.setByteArray("Blocks", abyte1);
				nbttagcompound.setByteArray("Data", nibblearray.getData());
				nbttagcompound.setByteArray("SkyLight", nibblearray1.getData());
				nbttagcompound.setByteArray("BlockLight", nibblearray2.getData());
				nbttaglist.appendTag(nbttagcompound);
			}
		}

		nbt.setTag("Sections", nbttaglist);
		byte[] abyte = new byte[256];
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

		for (int l2 = 0; l2 < 16; ++l2) {
			for (int i3 = 0; i3 < 16; ++i3) {
				blockpos$mutableblockpos.setXyz(p_76690_0_.x << 4 | l2, 0, p_76690_0_.z << 4 | i3);
				abyte[i3 << 4 | l2] = (byte) (biomer.getBiome(blockpos$mutableblockpos).getLegacyId() & 255);
			}
		}

		nbt.setByteArray("Biomes", abyte);
		nbt.setTag("Entities", p_76690_0_.entities);
		nbt.setTag("TileEntities", p_76690_0_.tileEntities);

		if (p_76690_0_.tileTicks != null) {
			nbt.setTag("TileTicks", p_76690_0_.tileTicks);
		}
	}

	private static class AnvilConverterData {
		public long lastUpdated;
		public boolean terrainPopulated;
		public byte[] heightmap;
		public NibbleArrayReader blockLight;
		public NibbleArrayReader skyLight;
		public NibbleArrayReader data;
		public byte[] blocks;
		public NBTTagList entities;
		public NBTTagList tileEntities;
		public NBTTagList tileTicks;
		public final int x;
		public final int z;

		public AnvilConverterData(int x, int z) {
			this.x = x;
			this.z = z;
		}
	}
}
