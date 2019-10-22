package net.minecraft.util;

public class AxisAlignedBB {

	public final double minX;
	public final double minY;
	public final double minZ;
	public final double maxX;
	public final double maxY;
	public final double maxZ;

	public AxisAlignedBB(double x1, double y1, double z1, double x2, double y2, double z2) {
		this.minX = Math.min(x1, x2);
		this.minY = Math.min(y1, y2);
		this.minZ = Math.min(z1, z2);
		this.maxX = Math.max(x1, x2);
		this.maxY = Math.max(y1, y2);
		this.maxZ = Math.max(z1, z2);
	}

	public AxisAlignedBB(BlockPos pos1, BlockPos pos2) {
		this.minX = (double) pos1.getX();
		this.minY = (double) pos1.getY();
		this.minZ = (double) pos1.getZ();
		this.maxX = (double) pos2.getX();
		this.maxY = (double) pos2.getY();
		this.maxZ = (double) pos2.getZ();
	}

	/**
	 * Adds the coordinates to the bounding box extending it if the point lies outside the current ranges. Args: x, y, z
	 */
	public AxisAlignedBB addCoord(double x, double y, double z) {
		double d0 = this.minX;
		double d1 = this.minY;
		double d2 = this.minZ;
		double d3 = this.maxX;
		double d4 = this.maxY;
		double d5 = this.maxZ;

		if (x < 0.0D) {
			d0 += x;
		} else if (x > 0.0D) {
			d3 += x;
		}

		if (y < 0.0D) {
			d1 += y;
		} else if (y > 0.0D) {
			d4 += y;
		}

		if (z < 0.0D) {
			d2 += z;
		} else if (z > 0.0D) {
			d5 += z;
		}

		return new AxisAlignedBB(d0, d1, d2, d3, d4, d5);
	}

	/**
	 * Returns a bounding box expanded by the specified vector (if negative numbers are given it will shrink). Args: x,
	 * y, z
	 */
	public AxisAlignedBB expand(double x, double y, double z) {
		double d0 = this.minX - x;
		double d1 = this.minY - y;
		double d2 = this.minZ - z;
		double d3 = this.maxX + x;
		double d4 = this.maxY + y;
		double d5 = this.maxZ + z;
		return new AxisAlignedBB(d0, d1, d2, d3, d4, d5);
	}

	public AxisAlignedBB union(AxisAlignedBB other) {
		double d0 = Math.min(this.minX, other.minX);
		double d1 = Math.min(this.minY, other.minY);
		double d2 = Math.min(this.minZ, other.minZ);
		double d3 = Math.max(this.maxX, other.maxX);
		double d4 = Math.max(this.maxY, other.maxY);
		double d5 = Math.max(this.maxZ, other.maxZ);
		return new AxisAlignedBB(d0, d1, d2, d3, d4, d5);
	}

	/**
	 * returns an AABB with corners x1, y1, z1 and x2, y2, z2
	 */
	public static AxisAlignedBB fromBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
		double d0 = Math.min(x1, x2);
		double d1 = Math.min(y1, y2);
		double d2 = Math.min(z1, z2);
		double d3 = Math.max(x1, x2);
		double d4 = Math.max(y1, y2);
		double d5 = Math.max(z1, z2);
		return new AxisAlignedBB(d0, d1, d2, d3, d4, d5);
	}

	/**
	 * Offsets the current bounding box by the specified coordinates. Args: x, y, z
	 */
	public AxisAlignedBB offset(double x, double y, double z) {
		return new AxisAlignedBB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
	}

	/**
	 * if instance and the argument bounding boxes overlap in the Y and Z dimensions, calculate the offset between them
	 * in the X dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
	 * calculated offset.  Otherwise return the calculated offset.
	 */
	public double calculateXOffset(AxisAlignedBB other, double offsetX) {
		if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
			if (offsetX > 0.0D && other.maxX <= this.minX) {
				double d1 = this.minX - other.maxX;

				if (d1 < offsetX) {
					offsetX = d1;
				}
			} else if (offsetX < 0.0D && other.minX >= this.maxX) {
				double d0 = this.maxX - other.minX;

				if (d0 > offsetX) {
					offsetX = d0;
				}
			}

			return offsetX;
		}
		return offsetX;
	}

	/**
	 * if instance and the argument bounding boxes overlap in the X and Z dimensions, calculate the offset between them
	 * in the Y dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
	 * calculated offset.  Otherwise return the calculated offset.
	 */
	public double calculateYOffset(AxisAlignedBB other, double offsetY) {
		if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
			if (offsetY > 0.0D && other.maxY <= this.minY) {
				double d1 = this.minY - other.maxY;

				if (d1 < offsetY) {
					offsetY = d1;
				}
			} else if (offsetY < 0.0D && other.minY >= this.maxY) {
				double d0 = this.maxY - other.minY;

				if (d0 > offsetY) {
					offsetY = d0;
				}
			}

			return offsetY;
		}
		return offsetY;
	}

	/**
	 * if instance and the argument bounding boxes overlap in the Y and X dimensions, calculate the offset between them
	 * in the Z dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
	 * calculated offset.  Otherwise return the calculated offset.
	 */
	public double calculateZOffset(AxisAlignedBB other, double offsetZ) {
		if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
			if (offsetZ > 0.0D && other.maxZ <= this.minZ) {
				double d1 = this.minZ - other.maxZ;

				if (d1 < offsetZ) {
					offsetZ = d1;
				}
			} else if (offsetZ < 0.0D && other.minZ >= this.maxZ) {
				double d0 = this.maxZ - other.minZ;

				if (d0 > offsetZ) {
					offsetZ = d0;
				}
			}

			return offsetZ;
		}
		return offsetZ;
	}

	/**
	 * Returns whether the given bounding box intersects with this one. Args: axisAlignedBB
	 */
	public boolean intersectsWith(AxisAlignedBB other) {
		return other.maxX > this.minX && other.minX < this.maxX ? other.maxY > this.minY && other.minY < this.maxY ? other.maxZ > this.minZ && other.minZ < this.maxZ : false : false;
	}

	/**
	 * Returns if the supplied Vec3D is completely inside the bounding box
	 */
	public boolean isVecInside(Vec3d vec) {
		return vec.xCoord > this.minX && vec.xCoord < this.maxX ? vec.yCoord > this.minY && vec.yCoord < this.maxY ? vec.zCoord > this.minZ && vec.zCoord < this.maxZ : false : false;
	}

	/**
	 * Returns the average length of the edges of the bounding box.
	 */
	public double getAverageEdgeLength() {
		double d0 = this.maxX - this.minX;
		double d1 = this.maxY - this.minY;
		double d2 = this.maxZ - this.minZ;
		return (d0 + d1 + d2) / 3.0D;
	}

	/**
	 * Returns a bounding box that is inset by the specified amounts
	 */
	public AxisAlignedBB contract(double x, double y, double z) {
		double d0 = this.minX + x;
		double d1 = this.minY + y;
		double d2 = this.minZ + z;
		double d3 = this.maxX - x;
		double d4 = this.maxY - y;
		double d5 = this.maxZ - z;
		return new AxisAlignedBB(d0, d1, d2, d3, d4, d5);
	}

	public MovingObjectPosition calculateIntercept(Vec3d vecA, Vec3d vecB) {
		Vec3d vec3D = vecA.getIntermediateWithXValue(vecB, this.minX);
		Vec3d vec31D = vecA.getIntermediateWithXValue(vecB, this.maxX);
		Vec3d vec32D = vecA.getIntermediateWithYValue(vecB, this.minY);
		Vec3d vec33D = vecA.getIntermediateWithYValue(vecB, this.maxY);
		Vec3d vec34D = vecA.getIntermediateWithZValue(vecB, this.minZ);
		Vec3d vec35D = vecA.getIntermediateWithZValue(vecB, this.maxZ);

		if (!this.isVecInYZ(vec3D)) {
			vec3D = null;
		}

		if (!this.isVecInYZ(vec31D)) {
			vec31D = null;
		}

		if (!this.isVecInXZ(vec32D)) {
			vec32D = null;
		}

		if (!this.isVecInXZ(vec33D)) {
			vec33D = null;
		}

		if (!this.isVecInXY(vec34D)) {
			vec34D = null;
		}

		if (!this.isVecInXY(vec35D)) {
			vec35D = null;
		}

		Vec3d vec36D = null;

		if (vec3D != null) {
			vec36D = vec3D;
		}

		if (vec31D != null && (vec36D == null || vecA.squareDistanceTo(vec31D) < vecA.squareDistanceTo(vec36D))) {
			vec36D = vec31D;
		}

		if (vec32D != null && (vec36D == null || vecA.squareDistanceTo(vec32D) < vecA.squareDistanceTo(vec36D))) {
			vec36D = vec32D;
		}

		if (vec33D != null && (vec36D == null || vecA.squareDistanceTo(vec33D) < vecA.squareDistanceTo(vec36D))) {
			vec36D = vec33D;
		}

		if (vec34D != null && (vec36D == null || vecA.squareDistanceTo(vec34D) < vecA.squareDistanceTo(vec36D))) {
			vec36D = vec34D;
		}

		if (vec35D != null && (vec36D == null || vecA.squareDistanceTo(vec35D) < vecA.squareDistanceTo(vec36D))) {
			vec36D = vec35D;
		}

		if (vec36D == null) {
			return null;
		}
		EnumFacing enumfacing = null;

		if (vec36D == vec3D) {
			enumfacing = EnumFacing.WEST;
		} else if (vec36D == vec31D) {
			enumfacing = EnumFacing.EAST;
		} else if (vec36D == vec32D) {
			enumfacing = EnumFacing.DOWN;
		} else if (vec36D == vec33D) {
			enumfacing = EnumFacing.UP;
		} else if (vec36D == vec34D) {
			enumfacing = EnumFacing.NORTH;
		} else {
			enumfacing = EnumFacing.SOUTH;
		}

		return new MovingObjectPosition(vec36D, enumfacing);
	}

	/**
	 * Checks if the specified vector is within the YZ dimensions of the bounding box. Args: Vec3D
	 */
	private boolean isVecInYZ(Vec3d vec) {
		return vec == null ? false : vec.yCoord >= this.minY && vec.yCoord <= this.maxY && vec.zCoord >= this.minZ && vec.zCoord <= this.maxZ;
	}

	/**
	 * Checks if the specified vector is within the XZ dimensions of the bounding box. Args: Vec3D
	 */
	private boolean isVecInXZ(Vec3d vec) {
		return vec == null ? false : vec.xCoord >= this.minX && vec.xCoord <= this.maxX && vec.zCoord >= this.minZ && vec.zCoord <= this.maxZ;
	}

	/**
	 * Checks if the specified vector is within the XY dimensions of the bounding box. Args: Vec3D
	 */
	private boolean isVecInXY(Vec3d vec) {
		return vec == null ? false : vec.xCoord >= this.minX && vec.xCoord <= this.maxX && vec.yCoord >= this.minY && vec.yCoord <= this.maxY;
	}

	public String toString() {
		return "box[" + this.minX + ", " + this.minY + ", " + this.minZ + " -> " + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
	}

	public boolean func_181656_b() {
		return Double.isNaN(this.minX) || Double.isNaN(this.minY) || Double.isNaN(this.minZ) || Double.isNaN(this.maxX) || Double.isNaN(this.maxY) || Double.isNaN(this.maxZ);
	}

}
