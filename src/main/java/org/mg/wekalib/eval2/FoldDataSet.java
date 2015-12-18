package org.mg.wekalib.eval2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.mg.javalib.util.ArrayUtil;
import org.mg.wekalib.eval2.util.Printer;

import weka.core.Instances;

public class FoldDataSet extends AbstractDataSet
{
	protected DataSet parent;
	protected int numFolds;
	protected long randomSeed;
	protected int fold;
	protected boolean train;

	private DataSet self;
	private Instances instances;

	public FoldDataSet(DataSet parent, int numFolds, long randomSeed, int fold, boolean train)
	{
		this.parent = parent;
		this.numFolds = numFolds;
		this.randomSeed = randomSeed;
		this.fold = fold;
		this.train = train;
	}

	//	@Override
	//	public DataSet cloneDataset()
	//	{
	//		return new FoldDataSet(parent, numFolds, randomSeed, fold, train);
	//	}

	@Override
	public String key()
	{
		StringBuffer b = new StringBuffer();
		b.append(parent.key());
		b.append('#');
		b.append(numFolds);
		b.append('#');
		b.append(randomSeed);
		b.append('#');
		b.append(fold);
		b.append('#');
		b.append(train);
		return b.toString();
	}

	public int getFold()
	{
		return fold;
	}

	@Override
	public DataSet getFilteredDataset(String name, List<Integer> idx)
	{
		return getSelf().getFilteredDataset(name, idx);
	}

	@Override
	public int getSize()
	{
		return getSelf().getSize();
	}

	public DataSet getSelf()
	{
		if (self == null)
		{
			Printer.println("FoldDataset: creating " + (train ? "train" : "test") + " fold " + (fold + 1) + "/"
					+ numFolds + ", seed " + randomSeed);
			Integer[] idx = ArrayUtil.toIntegerArray(ArrayUtil.indexArray(parent.getSize()));
			ArrayUtil.scramble(idx, new Random(randomSeed));
			List<Integer[]> cvIdx = ArrayUtil.split(idx, numFolds);
			List<Integer> selfIdx = new ArrayList<>();
			for (int f = 0; f < cvIdx.size(); f++)
				if ((train && fold != f) || (!train && fold == f))
					selfIdx.addAll(ArrayUtil.toList(cvIdx.get(f)));
			self = parent.getFilteredDataset(getName(), selfIdx);
		}
		return self;
	}

	public String getName()
	{
		return (train ? "Train" : "Test") + " fold " + (fold + 1) + " of " + parent.getName();
	}

	@Override
	public Instances getWekaInstances()
	{
		if (instances == null)
			instances = getSelf().getWekaInstances();
		return instances;
	}

}
