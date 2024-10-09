defmodule Eloxir do
  import Scanner

  @moduledoc """
  Documentation for `Eloxir`.
  """

  @doc """
  Hello world.
  """
  def main(args) do
    # TODO command prompt for no args
    case args do
      [arg] ->
        run_file(arg)

      _ ->
        IO.puts(:stderr, "Usage: eloxir <file>")
        System.halt(64)
    end
  end

  defp run_file(path) do
    file = File.open!(path)
    stream = IO.stream(file, 1)
    scan(stream)
    File.close(file)
  end
end
