from datasets.generate_synthetic import generate


def test_synthetic_generator_is_reproducible_and_labeled() -> None:
    first = generate(120, 10, 42)
    second = generate(120, 10, 42)
    assert first.equals(second)
    assert int(first["is_anomaly"].sum()) == 10
