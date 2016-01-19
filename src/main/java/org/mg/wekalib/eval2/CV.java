package org.mg.wekalib.eval2;

import java.io.File;
import java.io.FileReader;

import org.mg.wekalib.eval2.data.DataSet;
import org.mg.wekalib.eval2.data.WekaInstancesDataSet;
import org.mg.wekalib.eval2.job.DataSetJobOwner;
import org.mg.wekalib.eval2.job.DefaultJobOwner;
import org.mg.wekalib.eval2.job.Printer;
import org.mg.wekalib.eval2.model.Model;
import org.mg.wekalib.eval2.model.RandomForestModel;
import org.mg.wekalib.evaluation.PredictionUtil;
import org.mg.wekalib.evaluation.Predictions;

import weka.core.Instances;

public class CV extends DefaultJobOwner<Predictions> implements DataSetJobOwner<Predictions>
{
	DataSet dataSet;
	Model model;
	int numFolds = 10;
	long randomSeed = 1;
	boolean stratified = false;

	public CV cloneJob()
	{
		CV cv = new CV();
		cv.setDataSet(dataSet);
		cv.setModel(model);
		cv.setNumFolds(numFolds);
		cv.setRandomSeed(randomSeed);
		cv.setStratified(stratified);
		return cv;
	}

	private Model getModel(int fold)
	{
		Model m = (Model) model.cloneJob();
		m.setTrainingDataset(dataSet.getTrainFold(numFolds, stratified, randomSeed, fold));
		m.setTestDataset(dataSet.getTestFold(numFolds, stratified, randomSeed, fold));
		return m;
	}

	@Override
	public String getName()
	{
		return "CV: numFolds " + numFolds + ", seed " + randomSeed;
	}

	@Override
	public String getKeyPrefix()
	{
		String strat = "";
		if (stratified)
			strat = "-strat";
		return "CV-numFolds" + numFolds + "-seed" + randomSeed + strat + File.separator
				+ model.getKeyPrefix()
				+ (dataSet != null ? (File.separator + dataSet.getKeyPrefix()) : "");
	}

	@Override
	public String getKeyContent()
	{
		// for retain old keys, use stratified in key only if enabled
		if (stratified)
			return getKeyContent(dataSet, model, numFolds, randomSeed, stratified);
		else
			return getKeyContent(dataSet, model, numFolds, randomSeed);
	}

	@Override
	public Runnable nextJob() throws Exception
	{
		if (dataSet == null)
			throw new NullPointerException("set dataset first");

		// run cv
		boolean allDone = true;
		for (int f = 0; f < numFolds; f++)
		{
			Model m = getModel(f);
			if (!m.isDone())
			{
				allDone = false;
				Runnable r = m.nextJob();
				if (r != null)
					return Printer.wrapRunnable(
							"CV: fold " + (f + 1) + "/" + numFolds + ", seed " + randomSeed, r);
			}
		}

		if (allDone)
			return blockedJob("CV: storing results", new Runnable()
			{
				@Override
				public void run()
				{
					store();
				}
			});
		else
			return null;
	}

	private void store()
	{
		Predictions pred = new Predictions();
		for (int f = 0; f < numFolds; f++)
		{
			Predictions p = getModel(f).getResult();
			for (int i = 0; i < p.actual.length; i++)
				p.fold[i] = f;
			pred = PredictionUtil.concat(pred, p);
		}
		//		System.err.println(PredictionUtil.summaryClassification(pred));
		setResult(pred);
	}

	public Model getModel()
	{
		return model;
	}

	public void setModel(Model mod)
	{
		model = mod;
	}

	@Override
	public void setDataSet(DataSet data)
	{
		dataSet = data;
	}

	public long getRandomSeed()
	{
		return randomSeed;
	}

	public void setRandomSeed(long r)
	{
		randomSeed = r;
	}

	public void setNumFolds(int numFolds)
	{
		this.numFolds = numFolds;
	}

	public void setStratified(boolean stratified)
	{
		this.stratified = stratified;
	}

	public static void main(String[] args) throws Exception
	{
		CV cv = new CV();
		Instances inst = new Instances(
				new FileReader("/home/martin/data/weka/nominal/breast-w.arff"));
		inst.setClassIndex(inst.numAttributes() - 1);
		cv.setDataSet(new WekaInstancesDataSet(inst, 1));
		cv.setModel(new RandomForestModel());
		cv.runSequentially();
		System.out.println(PredictionUtil.summaryClassification(cv.getResult()));

	}

	public DataSet getDataset()
	{
		return dataSet;
	}
}
